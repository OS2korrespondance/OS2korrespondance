package dk.digitalidentity.medcommailbox.model.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Status for en enkelt mappesti")
public class IsAlivePathStatus {

	@Schema(description = "Navn på mappen fra konfigurationen", example = "MedcomIn")
	private String name;

	@Schema(description = "Fuld sti til mappen", example = "C:/medcom-file-sync/sync/frakmd")
	private String directoryPath;

	@Schema(description = "Om mappen er tilgængelig og læsbar", example = "true")
	private boolean accessible;
}
