package dk.digitalidentity.medcommailbox.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S3 {
	private String bucketName;
	private String binDirectory;
	private String inDirectory;
	private String outDirectory;
	private String archiveDirectory;
}
