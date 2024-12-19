package dk.digitalidentity.medcommailbox.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.dao.FailedS3KeyDao;
import dk.digitalidentity.medcommailbox.dao.model.FailedS3Key;

@Service
public class FailedS3KeyService {
	
	@Autowired
	private FailedS3KeyDao failedS3KeyDao;
	
	public FailedS3Key getByS3FileKey(String s3Key) {
		return failedS3KeyDao.findByS3FileKey(s3Key);
	}
	
	public FailedS3Key save(FailedS3Key failedS3Key) {
		return failedS3KeyDao.save(failedS3Key);
	}
}