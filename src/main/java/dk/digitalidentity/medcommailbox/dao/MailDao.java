package dk.digitalidentity.medcommailbox.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MailDao extends JpaRepository<Mail, Long> {
	List<Mail> findByFolderAndDraftFalseOrderByReceivedDesc(Folder folder);
	long countByReadAndFolderAndDraftFalse(boolean read, Folder folder);
	long countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(Folder folder);
	long countByFolderAndDraftFalse(Folder folder);
	Mail findById(long id);
	long deleteByDraftTrueAndCreatedBefore(LocalDateTime before);
	List<Mail> findByOriginalFolderAndDraftFalseAndS3FileKeyNotNull(Folder folder);
	@Query("select a from Mail a join MedcomLog l on l.envelopeIdentifier=:envelopeIdentifier and l.letterIdentifier=:letterIdentifier " +
			"where l.incomming=:incoming and a.envelopeIdentifier=:envelopeIdentifier and a.letterIdentifier=:letterIdentifier group by l.id")
	Mail findByEnvelopeIdentifierAndLetterIdentifierAndIncoming(@Param("envelopeIdentifier") String envelopeIdentifier,
																@Param("letterIdentifier") String letterIdentifier,
																@Param("incoming") Boolean incoming);
	List<Mail> findByDeletedDateBeforeAndFolder(LocalDate before, Folder folder);

	Optional<Mail> findFirstByOriginalFolderAndReferencesObjectIdentifier(Folder originalFolder, @Param("objectIdentifier") final String objectIdentifier);

	long countByRecipientIs(final Recipient recipient);
}
