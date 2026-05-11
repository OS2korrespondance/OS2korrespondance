package dk.digitalidentity.medcommailbox.service.ssr;

import dk.digitalidentity.medcommailbox.service.ssr.dto.DoctorLookupDto;
import java.util.Optional;

public interface ISSRService {
	Optional<DoctorLookupDto> lookupDoctor(String cpr);
}
