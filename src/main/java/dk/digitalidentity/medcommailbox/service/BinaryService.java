package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.BinaryDao;
import dk.digitalidentity.medcommailbox.dao.model.Binary;
import dk.digitalidentity.medcommailbox.dao.model.Reference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static dk.digitalidentity.medcommailbox.util.EmessageUtil.makeValidUUID;

@Service
public class BinaryService {
    @Autowired
    private BinaryDao binaryDao;
    @Autowired
    private S3Service s3Service;

    /**
     * Finds a binary by identifier, the identifier is always UUID, but in references there is no dashes,
     * so it will add dashes if they are missing.
     */
    public Optional<Binary> findByIdentifier(final String identifier) {
        return binaryDao.findFirstByIdentifier(makeValidUUID(identifier));
    }

    public Binary save(final Binary binary) {
        return binaryDao.save(binary);
    }

    public void delete(final Binary binary) {
        s3Service.delete(binary.getS3FileKey());
        binaryDao.delete(binary);
    }

    public String getReferenceFilename(final Reference reference) {
        if (!StringUtils.isEmpty(reference.getFilename())) {
            return reference.getFilename();
        }
        if (!StringUtils.isEmpty(reference.getReferenceDescription())) {
            return reference.getReferenceDescription();
        }
        if (!StringUtils.isEmpty(reference.getObjectExtensionCode())) {
            return reference.getObjectIdentifier() + "." + reference.getObjectExtensionCode();
        }
//        if (!StringUtils.isEmpty(reference.getObjectCode()) && !StringUtils.isEmpty(reference.getObjectExtensionCode())) {
//            return reference.getObjectCode() + "." + reference.getObjectExtensionCode();
//        }
        if (!StringUtils.isEmpty(reference.getObjectExtensionCode())) {
            return "file" + "." + reference.getObjectExtensionCode();
        }
        return "ukendfilenavn";
    }


}
