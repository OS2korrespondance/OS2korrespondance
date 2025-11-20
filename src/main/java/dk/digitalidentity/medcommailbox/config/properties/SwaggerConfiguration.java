package dk.digitalidentity.medcommailbox.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SwaggerConfiguration {
    private String username;
    private String password;
	private String apiKey;
	private boolean enabled;
}
