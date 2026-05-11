package dk.digitalidentity.medcommailbox.model.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fejlrapport fra MedcomFileSync")
public record ErrorReportRequest(
		@Schema(description = "Navnet på det job der fejlede", example = "FileUploadJob")
		String jobName,

		@Schema(description = "Fejlbeskeden", example = "Failed to execute FileUpload job for MedcomIn")
		String errorMessage
) {}
