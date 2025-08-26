package dk.digitalidentity.medcommailbox.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.medcommailbox.config.properties.Cpr;
import dk.digitalidentity.medcommailbox.config.properties.Email;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "medcom-mailbox")
public class MedcomMailboxConfiguration {
	private boolean schedulingEnabled = true;
	private S3 s3 = new S3();
	private List<Sender> senders = new ArrayList<>();
	private int daysBeforeDeletion = 30;
	private String municipality;
	private String logDeleteAfter = "13M";
	private String adminRoleId;
	private String userRoleId;
	private String locationNumberConstraintName = "http://os2korrespondance.dk/constraints/lokationsnummer/1";
	private boolean allowAttachments = false;
	private boolean createArchives = true;
	private String s3EncryptionKey = null;
	private Cpr cpr = new Cpr();
	private Email email = new Email();
	private boolean downloadAsPdf = true;
}