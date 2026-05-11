package dk.digitalidentity.medcommailbox.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sygesikring {
	private boolean enabled = false;
	private String endpointUrl;
	private String keystorePath;
	private String keystorePassword;
	private String serviceAgreementUuid;
	private String userSystemUuid;
	private int connectTimeoutMs = 3000;
	private int readTimeoutMs = 5000;
}
