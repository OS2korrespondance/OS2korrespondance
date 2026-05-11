package dk.digitalidentity.medcommailbox.service.ssr.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorLookupDto {
	private String ydernummer;
	private String praksisbetegnelse;
	private String adresse;
	private String telefon;
}
