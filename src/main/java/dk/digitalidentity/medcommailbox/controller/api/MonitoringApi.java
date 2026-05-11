package dk.digitalidentity.medcommailbox.controller.api;

import dk.digitalidentity.medcommailbox.model.api.ErrorReportRequest;
import dk.digitalidentity.medcommailbox.model.api.IsAliveRequest;
import dk.digitalidentity.medcommailbox.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring resource")
@RequiredArgsConstructor
public class MonitoringApi {

	private final MonitoringService monitoringService;

	@PostMapping("/isalive")
	@Operation(
			summary = "Rapportér IsAlive status fra MedcomFileSync",
			description = """
        Kaldes periodisk af MedcomFileSync-klienten for at rapportere at servicen kører.
        Medsendes oppetid, konfigurerede mappestier og om der er adgang til dem.
        Hvis oppetiden falder (genstart), stier ændres, eller mapper er utilgængelige,
        sendes en alarm til de konfigurerede modtagere.
        """
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "IsAlive status modtaget"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Ugyldig eller manglende ApiKey"
			)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "IsAlive data fra MedcomFileSync-klienten",
			required = true,
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
                {
                  "uptimeSeconds": 16200,
                  "paths": [
                    {
                      "name": "MedcomIn",
                      "directoryPath": "C:/medcom-file-sync/sync/frakmd",
                      "accessible": true
                    },
                    {
                      "name": "MedcomOut",
                      "directoryPath": "C:/medcom-file-sync/sync/tilkmd",
                      "accessible": true
                    },
                    {
                      "name": "MedcomArkiv",
                      "directoryPath": "C:/medcom-file-sync/sync/arkiv",
                      "accessible": true
                    }
                  ]
                }
            """)
			)
	)
	public ResponseEntity<Void> reportIsAlive(@RequestBody IsAliveRequest request) {
		monitoringService.registerIsAlive(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/error")
	@Operation(
			summary = "Rapportér fejl fra MedcomFileSync",
			description = "Kaldes af MedcomFileSync når der opstår en fejl under sync. Udløser en alarm til konfigurerede modtagere."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Fejl modtaget"),
			@ApiResponse(responseCode = "401", description = "Ugyldig eller manglende ApiKey")
	})
	public ResponseEntity<Void> reportError(@RequestBody ErrorReportRequest request) {
		monitoringService.handleErrorReport(request);
		return ResponseEntity.ok().build();
	}
}
