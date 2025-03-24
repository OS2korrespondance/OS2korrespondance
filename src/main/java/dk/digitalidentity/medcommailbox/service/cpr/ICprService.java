package dk.digitalidentity.medcommailbox.service.cpr;


import dk.digitalidentity.medcommailbox.service.cpr.dto.CprLookupDto;

public interface ICprService {
	CprLookupDto cprLookup(String cpr);

	boolean validCpr(String cpr);
}
