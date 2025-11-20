package dk.digitalidentity.medcommailbox.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AttachementResponse")
public class AttachmentResponseEO {
    @Schema(description = "Systemgenereret ID for den uploadede vedhæftning", example = "de305d54-75b4-431b-adb2-eb6b9e546014")
    private UUID attachementId;
    @Schema(description = "Klientgenereret ID som identificerer vedhæftningsgruppen", example = "abc-123")
    private String groupId;
    @Schema(description = "Tid i sekunder filen vil blive opbevaret midlertidigt", example = "3600")
    private Long keepAliveTime;
}
