package dk.digitalidentity.medcommailbox.controller.rest;

import dk.digitalidentity.medcommailbox.dao.model.InboxFolder;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.service.InboxFolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequireAdminAccess
@RequestMapping("/rest/folder")
public class FolderRestController {

	@Autowired
	private InboxFolderService inboxFolderService;


	@DeleteMapping("delete/{customFolderId}")
	public ResponseEntity<?> deleteCustomFolder(@PathVariable long customFolderId) {

		//find folder
		InboxFolder inboxFolder = inboxFolderService.findById(customFolderId);
		if (inboxFolder == null) {
			throw new IllegalArgumentException("Folder with this id not found");
		}

		//delete folder (and move all of its mails)
		inboxFolderService.delete(inboxFolder);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
