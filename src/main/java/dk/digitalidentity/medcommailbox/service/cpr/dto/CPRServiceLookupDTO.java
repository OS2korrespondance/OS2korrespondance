package dk.digitalidentity.medcommailbox.service.cpr.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CPRServiceLookupDTO {
	private String firstname;
	private String lastname;
	private String street;
	private String localname;
	private String postalCode;
	private String city;
	private String country;
	private boolean addressProtected;
}
