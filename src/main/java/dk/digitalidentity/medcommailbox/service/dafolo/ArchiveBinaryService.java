package dk.digitalidentity.medcommailbox.service.dafolo;

import dk.digitalidentity.medcommailbox.model.entity.Binary;
import dk.digitalidentity.medcommailbox.model.entity.Mail;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.service.dafolo.model.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching binary attachments from S3 for archiving purposes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveBinaryService {

	private final S3Service s3Service;

	/**
	 * Fetch all binaries from S3 for a mail message
	 *
	 * @param mail the mail with binary attachments
	 * @return BinaryData containing file contents and filenames mapped by binary ID
	 */
	public BinaryData fetchBinariesFromS3(Mail mail) {
		Map<Long, byte[]> attachments = new HashMap<>();
		Map<Long, String> attachmentFileNames = new HashMap<>();

		if (mail.getBinaryMessage() == null || mail.getBinaryMessage().getBinaries() == null) {
			log.debug("No binaries found for mail {}", mail.getId());
			return new BinaryData(attachments, attachmentFileNames);
		}

		List<Binary> binaries = mail.getBinaryMessage().getBinaries();

		for (Binary binary : binaries) {
			try {
				byte[] fileContent = s3Service.downloadFromS3(binary);

				if (fileContent != null) {
					String fileName = extractFileNameFromS3Key(binary.getS3FileKey());
					attachments.put(binary.getId(), fileContent);
					attachmentFileNames.put(binary.getId(), fileName);

					log.debug("Successfully fetched binary {} ({}) for mail {}",
							binary.getId(), fileName, mail.getId());
				} else {
					log.warn("Failed to download binary {} for mail {} - file content is null",
							binary.getId(), mail.getId());
				}
			} catch (Exception e) {
				log.error("Error downloading binary {} for mail {}", binary.getId(), mail.getId(), e);
				// Continue with other binaries even if one fails
			}
		}

		log.info("Fetched {}/{} binaries for mail {}",
				attachments.size(), binaries.size(), mail.getId());

		return new BinaryData(attachments, attachmentFileNames);
	}

	/**
	 * Extract filename from S3 key path
	 *
	 * @param s3FileKey the S3 file key
	 * @return the filename
	 */
	public String extractFileNameFromS3Key(String s3FileKey) {
		if (s3FileKey == null) {
			return "unknown";
		}

		// Remove .encrypted suffix if present
		String cleanKey = s3FileKey.endsWith(".encrypted")
				? s3FileKey.substring(0, s3FileKey.length() - ".encrypted".length())
				: s3FileKey;

		// Extract filename from path
		int lastSlashIndex = cleanKey.lastIndexOf('/');
		if (lastSlashIndex >= 0 && lastSlashIndex < cleanKey.length() - 1) {
			return cleanKey.substring(lastSlashIndex + 1);
		}

		return cleanKey;
	}

}