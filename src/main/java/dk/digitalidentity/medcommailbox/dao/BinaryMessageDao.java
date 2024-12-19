package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BinaryMessageDao extends CrudRepository<BinaryMessage, Long> {

    Optional<BinaryMessage> findBinariesByEnvelopeIdentifierAndLetterIdentifier(final String envelopeIdentifier, final String letterIdentifier);

}
