package dk.digitalidentity.medcommailbox.service.ssr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.properties.Sygesikring;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.cpr.ICprService;
import dk.digitalidentity.medcommailbox.service.ssr.dto.DoctorLookupDto;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.ServiceplatformFault;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerLaesPayloadResponseType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerLaesResponseType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SygesikringsoplysningerServicePort;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.SikretType;
import dk.digitalidentity.medcommailbox.serviceplatform.sygesikring.generated.YderType;
import jakarta.xml.ws.WebServiceException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SSRServiceTest {

	private static final String VALID_CPR = "0101011234";
	private static final String YDERNUMMER = "012345";
	private static final String PRAKSISBETEGNELSE = "Testlægehuset";

	@Mock
	private SygesikringsoplysningerServicePort mockPort;
	@Mock
	private ICprService iCprService;
	@Mock
	private SecurityUtil securityUtil;

	private SSRService service;
	private MedcomMailboxConfiguration configuration;

	@BeforeEach
	void setUp() {
		Sygesikring sygesikring = new Sygesikring();
		sygesikring.setEnabled(true);
		sygesikring.setServiceAgreementUuid("sa-uuid");
		sygesikring.setUserSystemUuid("us-uuid");
		configuration = new MedcomMailboxConfiguration();
		configuration.setSygesikring(sygesikring);

		service = new SSRService();
		ReflectionTestUtils.setField(service, "settings", configuration);
		ReflectionTestUtils.setField(service, "iCprService", iCprService);
		ReflectionTestUtils.setField(service, "securityUtil", securityUtil);
		ReflectionTestUtils.setField(service, "port", mockPort);
	}

	@Test
	void lookupDoctor_mapsNuvaerendeYderCorrectly() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		when(securityUtil.getUserId()).thenReturn("user-id");
		when(mockPort.sygesikringsoplysningerLaes(any())).thenReturn(buildResponse(YDERNUMMER, PRAKSISBETEGNELSE, "Testvej 1", "12345678"));

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isPresent();
		assertThat(result.get().getYdernummer()).isEqualTo(YDERNUMMER);
		assertThat(result.get().getPraksisbetegnelse()).isEqualTo(PRAKSISBETEGNELSE);
		assertThat(result.get().getAdresse()).isEqualTo("Testvej 1");
		assertThat(result.get().getTelefon()).isEqualTo("12345678");
	}

	@Test
	void lookupDoctor_whenNuvaerendeYderIsNull_returnsEmpty() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		when(securityUtil.getUserId()).thenReturn("user-id");
		when(mockPort.sygesikringsoplysningerLaes(any())).thenReturn(buildResponseNoGP());

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
	}

	@Test
	void lookupDoctor_whenServiceplatformFault_returnsEmpty() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		when(securityUtil.getUserId()).thenReturn("user-id");
		when(mockPort.sygesikringsoplysningerLaes(any())).thenThrow(new ServiceplatformFault("service fault"));

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
	}

	@Test
	void lookupDoctor_whenTransportException_returnsEmpty() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		when(securityUtil.getUserId()).thenReturn("user-id");
		when(mockPort.sygesikringsoplysningerLaes(any())).thenThrow(new WebServiceException("connection timed out"));

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
	}

	@Test
	void lookupDoctor_whenInvalidCpr_returnsEmptyWithoutCallingPort() throws ServiceplatformFault {
		when(iCprService.validCpr("notacpr")).thenReturn(false);

		Optional<DoctorLookupDto> result = service.lookupDoctor("notacpr");

		assertThat(result).isEmpty();
		verify(mockPort, never()).sygesikringsoplysningerLaes(any());
	}

	@Test
	void lookupDoctor_whenDisabled_returnsEmptyWithoutCallingPort() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		configuration.getSygesikring().setEnabled(false);

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
		verify(mockPort, never()).sygesikringsoplysningerLaes(any());
	}

	@Test
	void lookupDoctor_whenPortNotInitialized_returnsEmpty() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		ReflectionTestUtils.setField(service, "port", null);

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
		verify(mockPort, never()).sygesikringsoplysningerLaes(any());
	}

	@Test
	void lookupDoctor_whenNoAuthenticatedUser_returnsEmpty() throws ServiceplatformFault {
		when(iCprService.validCpr(VALID_CPR)).thenReturn(true);
		when(securityUtil.getUserId()).thenReturn(null);

		Optional<DoctorLookupDto> result = service.lookupDoctor(VALID_CPR);

		assertThat(result).isEmpty();
		verify(mockPort, never()).sygesikringsoplysningerLaes(any());
	}

	// --- helpers ---

	private SygesikringsoplysningerLaesResponseType buildResponse(String ydernummer, String praksisbetegnelse, String adresse, String telefon) {
		YderType yder = new YderType();
		yder.setYdernrYder(ydernummer);
		yder.setPrakBetegn(praksisbetegnelse);
		yder.setAdrYder(adresse);
		yder.setHvdTlf(telefon);

		SygesikringsoplysningerLaesPayloadResponseType payload = new SygesikringsoplysningerLaesPayloadResponseType();
		payload.setSikret(new SikretType());
		payload.setNuvaerendeYder(yder);

		SygesikringsoplysningerLaesResponseType response = new SygesikringsoplysningerLaesResponseType();
		response.setResponse(payload);
		return response;
	}

	private SygesikringsoplysningerLaesResponseType buildResponseNoGP() {
		SygesikringsoplysningerLaesPayloadResponseType payload = new SygesikringsoplysningerLaesPayloadResponseType();
		payload.setSikret(new SikretType());

		SygesikringsoplysningerLaesResponseType response = new SygesikringsoplysningerLaesResponseType();
		response.setResponse(payload);
		return response;
	}
}
