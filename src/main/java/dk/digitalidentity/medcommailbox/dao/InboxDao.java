package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboxDao extends JpaRepository<Inbox, Long> {

	Optional<Inbox> findByEanIdentifier(String eanIdentifier);

}
