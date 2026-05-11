package dk.digitalidentity.medcommailbox.model.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "IsAlive status fra MedcomFileSync-klienten")
public class IsAliveRequest {

	@Schema(description = "Antal sekunder servicen har kørt siden seneste opstart", example = "16200")
	private long uptimeSeconds;

	@Schema(description = "Konfigurerede mappestier og deres tilgængelighed")
	private List<IsAlivePathStatus> paths;
}
