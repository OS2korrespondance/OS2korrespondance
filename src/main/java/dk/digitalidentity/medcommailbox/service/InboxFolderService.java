package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.InboxFolderDao;
import dk.digitalidentity.medcommailbox.dao.model.InboxFolder;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InboxFolderService {

	@Autowired
	private InboxFolderDao inboxFolderDao;

	public InboxFolder findById(long id) {
		return inboxFolderDao.findById(id);
	}

	public List<InboxFolder> findAll() {
		return inboxFolderDao.findAll();
	}

	public InboxFolder save(InboxFolder inboxFolder) {
		return inboxFolderDao.save(inboxFolder);
	}

	/**
	 * Deletes the folder from the database, setting all of their inboxFolder to null
	 * @param inboxFolder
	 */
	public void delete(InboxFolder inboxFolder) {
		//move all mails to inbox
		for (Mail mail : inboxFolder.getMails()) {
			mail.setInboxFolder(null);
		}

		inboxFolderDao.delete(inboxFolder);
	}
}
