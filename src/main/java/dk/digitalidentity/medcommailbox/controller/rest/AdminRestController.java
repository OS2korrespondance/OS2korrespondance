package dk.digitalidentity.medcommailbox.controller.rest;

import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("ClassEscapesDefinedScope")
@RequireAdminAccess
@RestController
public class AdminRestController {

	@Autowired
	private RecipientService recipientService;
	@Autowired
	private AuditLogService auditLogService;

	record SetRecipientInput(@NotNull String shortName, @NotNull String fullName, @NotNull String ean){}
	@ResponseBody
	@PostMapping("/rest/admin/recipients/{ean}/name")
	public ResponseEntity<?> setRecipientName(@PathVariable String ean, @RequestBody SetRecipientInput setRecipientInput) {
		Recipient recipient = recipientService.findByEanIdentifier(ean).orElse(null);
		if (recipient == null) {
			return new ResponseEntity<>("Modtageren findes ikke", HttpStatus.NOT_FOUND);
		}

		if (setRecipientInput.shortName == null || setRecipientInput.fullName == null) {
			return new ResponseEntity<>("Modtageren skal have et navn", HttpStatus.BAD_REQUEST);
		}
		
		//prepare details before changing
		StringBuilder details = new StringBuilder();
		details.append("FullOrganizationName skiftet fra: " + recipient.getFullOrganisationName() + " til: " + setRecipientInput.fullName);
		details.append("\r\n");
		details.append("ShortOrganizationName skiftet fra " + recipient.getShortOrganisationName()  + " til: " + setRecipientInput.shortName);
		
		recipient.setShortOrganisationName(setRecipientInput.shortName);
		recipient.setFullOrganisationName(setRecipientInput.fullName);
		recipientService.save(recipient);
		
		auditLogService.log(AuditLogOperation.RECIPIENT_CHANGE_NAME, details.toString());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	record AddRecipient(@NotNull IdentifierCode identifierCode, @NotEmpty String identifier, @NotNull Long ean, @NotEmpty String fullName, @NotEmpty String shortName) {}
	@ResponseBody
	@PostMapping("/rest/admin/recipients")
	public ResponseEntity<?> setRecipientName(@Valid @RequestBody AddRecipient recipientReq) {
		final Recipient recipient = new Recipient();
		recipient.setIdentifier(recipientReq.identifier);
		recipient.setIdentifierCode(recipientReq.identifierCode);
		recipient.setShortOrganisationName(recipientReq.shortName);
		recipient.setFullOrganisationName(recipientReq.fullName);
		recipient.setEanIdentifier("" + recipientReq.ean);
		recipientService.save(recipient);
		auditLogService.log(AuditLogOperation.RECIPIENT_CREATED, auditLogService.getDetails(recipient));
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@Transactional
	@ResponseBody
	@DeleteMapping("/rest/admin/recipients/{ean}")
	public ResponseEntity<?> deleteRecipient(@PathVariable String ean) {
		Recipient recipient = recipientService.findByEanIdentifier(ean).orElse(null);
		if (recipient == null) {
			return new ResponseEntity<>("Modtageren findes ikke", HttpStatus.NOT_FOUND);
		}
		if (recipientService.isRecipientInUse(recipient)) {
			return new ResponseEntity<>("Modtageren anvendes, og kan derfor ikke slettes", HttpStatus.BAD_REQUEST);
		}
		recipientService.delete(recipient);
		auditLogService.log(AuditLogOperation.RECIPIENT_DELETED, auditLogService.getDetails(recipient));
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}


}
