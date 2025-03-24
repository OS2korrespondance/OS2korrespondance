package dk.digitalidentity.medcommailbox.service.cpr.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CprLookupDto {
	private String name;
	private String surname;
	private LocalDate birthday;
	private String address;
	private String city;
	private String postCode;
	private String cpr;
	private boolean show16 = false;
	private boolean show18 = false;

}
