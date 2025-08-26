package dk.digitalidentity.medcommailbox.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dk.digitalidentity.medcommailbox.dao.model.InboxFolder;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MailDao extends JpaRepository<Mail, Long> {
	List<Mail> findByFolderAndDraftFalseOrderByReceivedDesc(Folder folder);

	@Query(value = "select count(1) from Mail m where m.folder=:folder " +
			"and m.read=false and (m.associatedIdentifier in :locationsNumbers)")
	long countUnreadForRootFolder(@Param("folder") final Folder folder,
								  @Param("locationsNumbers") final Set<String> locationsNumbers);

	@Query(value = "select count(1) from Mail m where m.folder=:folder and m.read=false")
	long countUnreadForRootFolder(@Param("folder") final Folder folder);

	long countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(Folder folder);
	@Query(value = "select count(1) from Mail m where m.folder=:folder " +
			"and (m.associatedIdentifier in :locationsNumbers) and m.draft=false and m.receivedNegativeReceipt=true")
	long countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(@Param("folder") final Folder folder,
																  @Param("locationsNumbers") final Set<String> locationsNumbers);

	long countByFolderAndInboxFolderAndDraftFalse(Folder folder, final InboxFolder inboxFolder);
	@Query(value = "select count(1) from Mail m where m.folder=:folder and m.inboxFolder=:inboxFolder " +
			"and (m.associatedIdentifier in :locationsNumbers) and m.draft=false")
	long countByFolderAndInboxFolderAndDraftFalse(@Param("folder") final Folder folder,
												  @Param("inboxFolder") final InboxFolder inboxFolder,
												  @Param("locationsNumbers") final Set<String> locationsNumbers);

	long countByFolderAndDraftFalseAndInboxFolderNull(final Folder folder);

	@Query(value = "select count(1) from Mail m where m.folder=:folder " +
			"and (m.associatedIdentifier in :locationsNumbers) and m.inboxFolder is null and m.draft=false")
	long countByFolderAndDraftFalseAndInboxFolderNull(@Param("folder") final Folder folder,
													  @Param("locationsNumbers") final Set<String> locationsNumbers);

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
