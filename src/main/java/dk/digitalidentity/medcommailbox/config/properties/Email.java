package dk.digitalidentity.medcommailbox.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Email {
	private String from;
	private String username;
	private String password;
	private String host;
}
