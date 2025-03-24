package dk.digitalidentity.medcommailbox.service.cpr;


import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.medcommailbox.service.cpr.dto.CPRServiceLookupDTO;
import dk.digitalidentity.medcommailbox.service.cpr.dto.CprLookupDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("!CprServiceMock")
public class CprService extends AbstractCprService {

	@Autowired
	AuditLogService auditLogService;

	@Override
	public CprLookupDto cprLookup(String cpr) {
		if (!validCpr(cpr)) {
			return null;
		}
		
		var restTemplate = new RestTemplate();

		String cprResourceUrl = settings.getCpr().getUrl();
		if (!cprResourceUrl.endsWith("/")) {
			cprResourceUrl += "/";
		}
		cprResourceUrl += "api/person?cpr=" + cpr + "&cvr=" + settings.getCpr().getCvr();

		try {
			ResponseEntity<CPRServiceLookupDTO> response = restTemplate.getForEntity(cprResourceUrl, CPRServiceLookupDTO.class);
			CPRServiceLookupDTO dto = response.getBody();
			if(dto != null) {
				var result = new CprLookupDto();
				//Name
				result.setName(dto.getFirstname());
				result.setSurname(dto.getLastname());
				result.setCpr(cpr);
				auditLogService.log(AuditLogOperation.CPR_LOOKUP, "Cpr opslag på " + result.getName() + " " + result.getSurname());
				return result;
			}
			return null;
		}
		catch (RestClientException ex) {
			log.warn("Failed to lookup: " + LogHelper.SafeCPR(cpr), ex);

			return null;
		}
	}
}