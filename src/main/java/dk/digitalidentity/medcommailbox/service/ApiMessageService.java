package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.ApiMessageDao;
import dk.digitalidentity.medcommailbox.model.entity.ApiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiMessageService {
	private final ApiMessageDao apiMessageDao;
	private final S3Service s3Service;
	private final MedcomMailboxConfiguration configuration;

	public void save(ApiMessage apiMessage) {
		apiMessageDao.save(apiMessage);
	}

	public void delete(ApiMessage apiMessage) {
		apiMessageDao.delete(apiMessage);
	}

	public ApiMessage findByGroupId(String groupId) {
		return apiMessageDao.findByGroupId(groupId);
	}

	public byte[] getExisting(String groupId) {
		try {
			String key = configuration.getS3().getBinDirectory() + "/" + groupId + ".zip";
			if (s3Service.encrypting()) {
				key += ".encrypted";
			}
			return s3Service.downloadBytes(key);
		}
		catch (IOException e) {
			log.error("Could not download file from S3: " + e.getMessage());
			return null;
		}
	}

	@Transactional
	public ApiMessage createApiMessage(String uuid, String fileName, Long fileSize, byte[] file) {

		// Step 1, try and save the file to S3
		s3Service.upload(configuration.getS3().getBinDirectory(), uuid + ".zip", file);

		// Step 2, save in our own backend
		ApiMessage newMessage = new ApiMessage();

		newMessage.setGroupId(uuid);
		newMessage.setFileName(fileName);
		newMessage.setFileSize(fileSize);

		ApiMessage save = apiMessageDao.save(newMessage);
		log.debug("Created new ApiMessage with id: " + newMessage.getId());
		return save;
	}

	@Transactional
	public ApiMessage updateApiMessage(ApiMessage existingMessage, String newFileName, Long newFileSize, byte[] mergedFile) {

		// Step 1: Upload merged file to S3 (overwrites existing file)
		s3Service.upload(configuration.getS3().getBinDirectory(), existingMessage.getGroupId() + ".zip", mergedFile);

		// Step 2: Update the existing message with new metadata
		existingMessage.setFileName(existingMessage.getFileName() + "," + newFileName);
		existingMessage.setFileSize(existingMessage.getFileSize() + newFileSize);

		ApiMessage updated = apiMessageDao.save(existingMessage);
		log.debug("Updated ApiMessage with id: {} - new total size: {}", updated.getId(), updated.getFileSize());

		return updated;
	}

	public void deleteExpiredMessaged() {
		List<ApiMessage> expiredMessages = apiMessageDao.findByCreatedAtBefore(LocalDateTime.now().minusHours(1));
		log.debug("Deleting a total of {} expired messages!", expiredMessages.size());

		expiredMessages.forEach(message -> {
			try {
				s3Service.delete(configuration.getS3().getBinDirectory(), message.getGroupId() + ".zip");
				delete(message);
			}
			catch (Exception e) {
				log.error("Could not delete expired message from S3: " + e.getMessage());
			}
		});
	}
}