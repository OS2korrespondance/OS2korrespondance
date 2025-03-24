package dk.digitalidentity.medcommailbox.datatables;

import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.datatables.model.MailboxDatatableDTO;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.domain.Specification;

public interface MailDatatableDao extends DataTablesRepository<MailboxDatatableDTO, Long> {

	public static Specification<MailboxDatatableDTO> inFolder(Folder folder) {
		return (root, query, builder) -> {
			Root<MailboxDatatableDTO> mail = builder.treat(root, MailboxDatatableDTO.class);
			return builder.equal(mail.get("folder"), folder.toString());
		};
	}

	public static Specification<MailboxDatatableDTO> inboxFolderNull() {
		return (root, query, builder) -> {
			Root<MailboxDatatableDTO> mail = builder.treat(root, MailboxDatatableDTO.class);
			return builder.isNull(mail.get("inboxFolderId"));
		};
	}

	public static Specification<MailboxDatatableDTO> withInboxFolderId( Long inboxFolderId) {
		return (root, query, builder) -> {
			Root<MailboxDatatableDTO> mail = builder.treat(root, MailboxDatatableDTO.class);
			return builder.equal(mail.get("inboxFolderId"), inboxFolderId);
		};
	}
}
