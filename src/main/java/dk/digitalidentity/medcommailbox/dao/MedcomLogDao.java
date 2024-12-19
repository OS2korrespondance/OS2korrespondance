package dk.digitalidentity.medcommailbox.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;

public interface MedcomLogDao extends JpaRepository<MedcomLog, Long> {
	Optional<MedcomLog> findByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier);
	Optional<MedcomLog> findFirstByEnvelopeIdentifierAndLetterIdentifierOrderByIdDesc(String envelopeIdentifier, String letterIdentifier);
	MedcomLog findByReceiptS3FileKey(String receiptS3FileKey);
	List<MedcomLog> findAllByIncommingOrderByIdDesc(boolean incomming);
	MedcomLog findById(long id);
    void deleteByMailTtsBeforeOrReceiptTtsBefore(LocalDateTime deleteBeforeDate, LocalDateTime deleteBeforeDate2);
}
