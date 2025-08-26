package dk.digitalidentity.medcommailbox.service;

import java.util.List;
import java.util.Optional;

import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dk.digitalidentity.medcommailbox.dao.RecipientDao;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;

@Service
public class RecipientService {
	
	@Autowired
	private RecipientDao recipientDao;
	@Autowired
	private MailDao mailDao;
	
	public List<Recipient> getAll() {
		return recipientDao.findAll();
	}

	public Optional<Recipient> findByEanIdentifier(String eanIdentifier) {
		final Optional<Recipient> byEanIdentifier = recipientDao.findByIdentifierAndIdentifierCode(eanIdentifier, IdentifierCode.lokationsnummer);
		if (byEanIdentifier.isPresent()) {
			return byEanIdentifier;
		}
		return recipientDao.findFirstByEanIdentifier(eanIdentifier);
	}

	public Recipient save(Recipient recipient) {
		return recipientDao.save(recipient);
	}

	@Transactional
	public void delete(final Recipient recipient) {
		recipientDao.delete(recipient);
	}

	public boolean isRecipientInUse(final Recipient recipient) {
		return mailDao.countByRecipientIs(recipient) > 0;
	}

	public Optional<Recipient> findById(@NotNull Long id) {
		return recipientDao.findById(id);
	}

}