package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.dao.model.Recipient;

import java.util.Optional;

public interface RecipientDao extends JpaRepository<Recipient, Long> {

	Optional<Recipient> findByIdentifierAndIdentifierCode(String identifier, IdentifierCode code);
	Optional<Recipient> findFirstByEanIdentifier(String eanIdentifier);
}
