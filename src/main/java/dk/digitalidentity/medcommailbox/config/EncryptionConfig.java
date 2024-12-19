package dk.digitalidentity.medcommailbox.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Configuration
public class EncryptionConfig {

    @Bean
    @ConditionalOnProperty(prefix = "medcom-mailbox", name = "s3EncryptionKey")
    public SecretKeySpec secretKeySpec(final MedcomMailboxConfiguration configuration) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] key = sha.digest(configuration.getS3EncryptionKey().getBytes(StandardCharsets.UTF_8));
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

}
