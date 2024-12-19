package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.dao.model.Binary;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BinaryDao extends CrudRepository<Binary, Long> {

    Optional<Binary> findBinariesByS3FileKey(final String s3FileKey);

    Optional<Binary> findFirstByIdentifier(final String identifier);
}
