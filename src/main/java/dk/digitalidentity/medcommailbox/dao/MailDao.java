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
	Mail findById(long id);
	long deleteByDraftTrueAndCreatedBefore(LocalDateTime before);
	List<Mail> findByOriginalFolderAndDraftFalseAndS3FileKeyNotNull(Folder folder);
	Mail findByEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier);
	List<Mail> findByDeletedDateBeforeAndFolder(LocalDate before, Folder folder);

	Optional<Mail> findFirstByOriginalFolderAndReferencesObjectIdentifier(Folder originalFolder, @Param("objectIdentifier") final String objectIdentifier);

	long countByRecipientIs(final Recipient recipient);
}
