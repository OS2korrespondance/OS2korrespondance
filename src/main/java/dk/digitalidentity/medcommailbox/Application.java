package dk.digitalidentity.medcommailbox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.security.Security;

@EnableMethodSecurity
@SpringBootApplication(scanBasePackages = { "dk.digitalidentity" })
public class Application {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(Application.class, args);
	}
}
