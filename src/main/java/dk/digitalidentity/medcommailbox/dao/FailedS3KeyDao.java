package dk.digitalidentity.medcommailbox.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.model.entity.FailedS3Key;

public interface FailedS3KeyDao extends JpaRepository<FailedS3Key, Long> {
	FailedS3Key findByS3FileKey(String S3FileKey);
}
