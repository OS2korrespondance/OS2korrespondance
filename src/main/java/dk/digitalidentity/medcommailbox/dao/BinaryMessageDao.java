package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BinaryMessageDao extends CrudRepository<BinaryMessage, Long> {
    @Query("select m from BinaryMessage m join MedcomLog l on l.envelopeIdentifier=:envelopeIdentifier and l.letterIdentifier=:letterIdentifier " +
            "where l.incomming=:incoming and m.envelopeIdentifier=:envelopeIdentifier and m.letterIdentifier=:letterIdentifier")
    Optional<BinaryMessage> findBinariesByEnvelopeIdentifierAndLetterIdentifier(@Param("envelopeIdentifier") String envelopeIdentifier,
                                                                                @Param("letterIdentifier") String letterIdentifier,
                                                                                @Param("incoming") Boolean incoming);

}
