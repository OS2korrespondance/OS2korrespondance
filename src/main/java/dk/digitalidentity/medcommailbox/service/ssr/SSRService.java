package dk.digitalidentity.medcommailbox.service.ssr;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.properties.Sygesikring;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.cpr.ICprService;
import dk.digitalidentity.medcommailbox.service.cpr.LogHelper;
import dk.digitalidentity.medcommailbox.service.ssr.dto.DoctorLookupDto;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.ServiceplatformFault;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerLaesPayloadRequestType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerLaesRequestType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerLaesResponseType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerService;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerServicePort;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.YderType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.invocationcontext.InvocationContextType;
import jakarta.annotation.PostConstruct;
import jakarta.xml.ws.WebServiceException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Optional;
import java.util.UUID;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!SSRServiceMock")
public class SSRService implements ISSRService {

	// Fast UUID for SF0830 v3.0 fra Kombits servicebeskrivelse — ikke kunde-/miljø-specifik.
	private static final String SF0830_SERVICE_UUID = "14dd1add-1459-4bae-b80b-26b4c36b1c81";

	@Autowired
	private MedcomMailboxConfiguration settings;

	@Autowired
	private ICprService iCprService;

	@Autowired
	private SecurityUtil securityUtil;

	private SygesikringsoplysningerServicePort port;

	@PostConstruct
	void init() {
		Sygesikring config = settings.getSygesikring();
		if (!config.isEnabled()) {
			log.info("SF0830: Sygesikring lookup disabled — SOAP port not initialized");
			return;
		}
		try {
			this.port = createConfiguredPort(config);
		} catch (Exception e) {
			log.error("SF0830: failed to initialize SOAP port — lookups will return empty", e);
		}
	}

	@Override
	public Optional<DoctorLookupDto> lookupDoctor(String cpr) {
		if (!iCprService.validCpr(cpr)) {
			return Optional.empty();
		}

		Sygesikring config = settings.getSygesikring();
		if (!config.isEnabled()) {
			return Optional.empty();
		}
		if (port == null) {
			log.warn("SF0830: lookup attempted but SOAP port not initialized");
			return Optional.empty();
		}

		String userUuid = currentUserUuid();
		if (userUuid == null) {
			log.warn("SF0830: lookup attempted without an authenticated user — refusing call");
			return Optional.empty();
		}

		try {
			SygesikringsoplysningerLaesPayloadRequestType payload = new SygesikringsoplysningerLaesPayloadRequestType();
			payload.setPNR(cpr);

			SygesikringsoplysningerLaesRequestType request = new SygesikringsoplysningerLaesRequestType();
			request.setInvocationContext(buildInvocationContext(config, userUuid));
			request.setRequest(payload);

			SygesikringsoplysningerLaesResponseType response = port.sygesikringsoplysningerLaes(request);

			YderType yder = response.getResponse().getNuvaerendeYder();
			if (yder == null) {
				log.info("SF0830: no current GP found for CPR {}", LogHelper.SafeCPR(cpr));
				return Optional.empty();
			}

			return Optional.of(DoctorLookupDto.builder()
					.ydernummer(yder.getYdernrYder())
					.praksisbetegnelse(yder.getPrakBetegn())
					.adresse(yder.getAdrYder())
					.telefon(yder.getHvdTlf())
					.build());

		} catch (ServiceplatformFault e) {
			log.warn("SF0830 ServiceplatformFault for CPR {}", LogHelper.SafeCPR(cpr), e);
			return Optional.empty();
		} catch (WebServiceException e) {
			log.warn("SF0830 transport failure for CPR {}", LogHelper.SafeCPR(cpr), e);
			return Optional.empty();
		} catch (RuntimeException e) {
			log.warn("SF0830 unexpected error for CPR {}", LogHelper.SafeCPR(cpr), e);
			return Optional.empty();
		}
	}

	private SygesikringsoplysningerServicePort createConfiguredPort(Sygesikring config) throws Exception {
		SygesikringsoplysningerServicePort newPort = new SygesikringsoplysningerService().getSygesikringsoplysningerServicePort();
		configureTransport(newPort, config);
		return newPort;
	}

	private void configureTransport(SygesikringsoplysningerServicePort port, Sygesikring config) throws Exception {
		Client cxfClient = ClientProxy.getClient(port);
		HTTPConduit conduit = (HTTPConduit) cxfClient.getConduit();

		HTTPClientPolicy httpPolicy = new HTTPClientPolicy();
		httpPolicy.setConnectionTimeout(config.getConnectTimeoutMs());
		httpPolicy.setReceiveTimeout(config.getReadTimeoutMs());
		conduit.setClient(httpPolicy);

		conduit.getTarget().getAddress().setValue(config.getEndpointUrl());

		TLSClientParameters tls = new TLSClientParameters();
		tls.setKeyManagers(loadKeyManagers(config));
		conduit.setTlsClientParameters(tls);
	}

	private KeyManager[] loadKeyManagers(Sygesikring config) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		try (InputStream is = new FileInputStream(config.getKeystorePath())) {
			keyStore.load(is, config.getKeystorePassword().toCharArray());
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, config.getKeystorePassword().toCharArray());
		return kmf.getKeyManagers();
	}

	private InvocationContextType buildInvocationContext(Sygesikring config, String userUuid) {
		InvocationContextType ctx = new InvocationContextType();
		ctx.setServiceAgreementUUID(config.getServiceAgreementUuid());
		ctx.setUserSystemUUID(config.getUserSystemUuid());
		ctx.setServiceUUID(SF0830_SERVICE_UUID);
		ctx.setUserUUID(userUuid);
		return ctx;
	}

	// Serviceplatformens audit-log skal pege på sagsbehandleren der laver opslaget. Vi
	// har ikke en stabil UUID i SAML-claims, så vi udleder en deterministisk UUIDv3 ud
	// fra det principal-navn vi har i forvejen. Samme bruger → samme UUID på tværs af
	// requests og restarts.
	private String currentUserUuid() {
		String userId = securityUtil.getUserId();
		if (userId == null || userId.isBlank()) {
			return null;
		}
		return UUID.nameUUIDFromBytes(userId.getBytes(StandardCharsets.UTF_8)).toString();
	}
}
