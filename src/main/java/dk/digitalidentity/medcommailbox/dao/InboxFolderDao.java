package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.InboxFolder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxFolderDao extends JpaRepository<InboxFolder, Long> {
	InboxFolder findById(long id);
}
