package dk.digitalidentity.medcommailbox.service.ssr;

import dk.digitalidentity.medcommailbox.service.cpr.ICprService;
import dk.digitalidentity.medcommailbox.service.ssr.dto.DoctorLookupDto;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("SSRServiceMock")
public class SSRServiceMock implements ISSRService {

	@Autowired
	private ICprService iCprService;

	@Override
	public Optional<DoctorLookupDto> lookupDoctor(String cpr) {
		if (!iCprService.validCpr(cpr)) {
			return Optional.empty();
		}
		return Optional.of(DoctorLookupDto.builder()
				.ydernummer("012345")
				.praksisbetegnelse("Testlægehuset")
				.adresse("Testvej 1")
				.telefon("12345678")
				.build());
	}
}
