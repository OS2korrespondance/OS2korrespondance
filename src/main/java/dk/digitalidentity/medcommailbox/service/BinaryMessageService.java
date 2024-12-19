package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.BinaryMessageDao;
import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BinaryMessageService {
    @Autowired
    private BinaryMessageDao binaryMessageDao;

    public BinaryMessage save(final BinaryMessage message) {
        return binaryMessageDao.save(message);
    }

}
