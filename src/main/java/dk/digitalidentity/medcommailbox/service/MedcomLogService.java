package dk.digitalidentity.medcommailbox.service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.dao.MedcomLogDao;
import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.util.DurationUtil;
import jakarta.transaction.Transactional;

@Service
public class MedcomLogService {
	
	@Autowired
	private MedcomLogDao logDao;
	
	public List<MedcomLog> getAll() {
		return logDao.findAll();
	}
	
	public List<MedcomLog> getByIncomming(boolean incomming) {
		return logDao.findAllByIncommingOrderByIdDesc(incomming);
	}
	
	public Optional<MedcomLog> getByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier) {
		return logDao.findByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(envelopeIdentifier, letterIdentifier);
	}

	public Optional<MedcomLog> getByEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier) {
		return logDao.findFirstByEnvelopeIdentifierAndLetterIdentifierOrderByIdDesc(envelopeIdentifier, letterIdentifier);
	}

	public MedcomLog getByReceiptS3FileKey(String receiptS3FileKey) {
		return logDao.findByReceiptS3FileKey(receiptS3FileKey);
	}
	
	public MedcomLog getById(long id) {
		return logDao.findById(id);
	}
	
	public MedcomLog save(MedcomLog log) {
		return logDao.save(log);
	}

	@Transactional
	public void deleteOldLogs(String deleteAfter) {
		TemporalAmount amount = DurationUtil.parse(deleteAfter);
		LocalDateTime dateNow = LocalDateTime.now();
		LocalDateTime deleteBeforeDate = dateNow.minus(amount);

		logDao.deleteByMailTtsBeforeOrReceiptTtsBefore(deleteBeforeDate, deleteBeforeDate);
	}


}