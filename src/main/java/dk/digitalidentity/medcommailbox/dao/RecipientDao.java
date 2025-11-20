package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.enums.IdentifierCode;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.model.entity.Recipient;

import java.util.Optional;

public interface RecipientDao extends JpaRepository<Recipient, Long> {

	Optional<Recipient> findByIdentifierAndIdentifierCode(String identifier, IdentifierCode code);
	Optional<Recipient> findFirstByEanIdentifier(String eanIdentifier);
}
