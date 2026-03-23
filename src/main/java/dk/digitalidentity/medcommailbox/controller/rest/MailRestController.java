package dk.digitalidentity.medcommailbox.controller.rest;

import static dk.digitalidentity.medcommailbox.Constants.FOLDER_CREATE_ID;
import static dk.digitalidentity.medcommailbox.Constants.FOLDER_INBOX_ID;
import static dk.digitalidentity.medcommailbox.util.UuidDash.removeDashes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import dk.digitalidentity.medcommailbox.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.model.entity.Binary;
import dk.digitalidentity.medcommailbox.model.entity.BinaryMessage;
import dk.digitalidentity.medcommailbox.model.entity.InboxFolder;
import dk.digitalidentity.medcommailbox.model.entity.Mail;
import dk.digitalidentity.medcommailbox.model.entity.Patient;
import dk.digitalidentity.medcommailbox.model.entity.Recipient;
import dk.digitalidentity.medcommailbox.model.entity.MessageTemplate;
import dk.digitalidentity.medcommailbox.model.entity.Reference;
import dk.digitalidentity.medcommailbox.model.entity.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.model.entity.enums.EpisodeOfCareStatusCode;
import dk.digitalidentity.medcommailbox.model.entity.enums.Folder;
import dk.digitalidentity.medcommailbox.model.entity.enums.ReferenceType;
import dk.digitalidentity.medcommailbox.model.entity.enums.Status;
import dk.digitalidentity.medcommailbox.datatables.MailDatatableDao;
import dk.digitalidentity.medcommailbox.datatables.model.MailboxDatatableDTO;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.medcommailbox.service.BinaryMessageService;
import dk.digitalidentity.medcommailbox.service.BinaryService;
import dk.digitalidentity.medcommailbox.controller.rest.dto.AutoReplyForm;
import dk.digitalidentity.medcommailbox.model.entity.Inbox;
import dk.digitalidentity.medcommailbox.service.InboxFolderService;
import dk.digitalidentity.medcommailbox.service.InboxSubscriberService;
import jakarta.transaction.Transactional;
import dk.digitalidentity.medcommailbox.service.MailService;
import dk.digitalidentity.medcommailbox.service.MedcomSenderService;
import dk.digitalidentity.medcommailbox.service.MessageTemplateService;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.service.cpr.ICprService;
import dk.digitalidentity.medcommailbox.service.cpr.dto.CprLookupDto;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectExtensionCodeType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireUserAccess
public class MailRestController {

	@Autowired
	private MailService mailService;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private RecipientService recipientService;
	@Autowired
	private AuditLogService auditLogService;
	@Autowired
	private InboxFolderService inboxFolderService;
	@Autowired
	private MedcomMailboxConfiguration configuration;
	@Autowired
	private EmessageMapper emessageMapper;
	@Autowired
	private BinaryService binaryService;
	@Autowired
	private BinaryMessageService binaryMessageService;
	@Autowired
	private MedcomSenderService senderService;
	@Autowired
	private ICprService iCprService;
	@Autowired
	private MessageTemplateService messageTemplateService;

	@Autowired
	private UserSession userSession;

	@Autowired
	private InboxSubscriberService inboxSubscriberService;

	record AttachemntDTO(MultipartFile file) {
	}

	record MailDTO(String subject, String content, String recipientEan, boolean answer, String patientStatus,
				   String patientName, String patientCpr, boolean highPriority, String senderIdentifier,
				   String caseId) {
	}

	/**
	 * Endpoint for the mailbox datatable
	 *
	 * @param input
	 * @param folder
	 * @param bindingResult
	 * @param inboxFolderId
	 * @param httpSession 
	 * @return
	 */
	@PostMapping("/rest/mail/{folder}")
	public DataTablesOutput<MailboxDatatableDTO> mailDatatable(@Valid @RequestBody DataTablesInput input, @PathVariable Folder folder, BindingResult bindingResult, @RequestParam(name = "folder", required = false) Long inboxFolderId, HttpSession httpSession) {
		DataTablesOutput<MailboxDatatableDTO> output = new DataTablesOutput<>();
		if (bindingResult.hasErrors()) {
			output.setError(bindingResult.toString());
			return output;
		}

		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);

		if (!constraints.isEmpty()) {
			String defaultInbox = (String) httpSession.getAttribute("selectedInbox");
			
			//limit mails to the selected inbox
			if (defaultInbox != null && !constraints.isEmpty()) {
				// remove all except selected constraint/inbox
				constraints.removeIf(constraint -> !constraint.equals(defaultInbox));
			}

			output = mailService.getByFolderAndConstraints(input, folder, constraints.toArray(new String[] {}));
		} else if (inboxFolderId == null) {
			output = mailService.getByFolder(input, folder);
		} else {
			output = mailService.getByFolder(input, folder, inboxFolderId);
		}

		//filter by user-created folder
		if (folder.equals(Folder.INBOX)) {
			//If id of custom folder is provided, find the folder and filter by that folder
			if (inboxFolderId != null) {
				InboxFolder inboxFolder = inboxFolderService.findById(inboxFolderId);
				if (inboxFolder != null) {
					output.setData(output.getData().stream()
							.filter(m -> m.getInboxFolderId() != null && m.getInboxFolderId().equals(inboxFolderId))
							.toList());
					return output;
				}
			}
			output.setData(output.getData().stream()
					.filter(m -> m.getInboxFolderId() == null)
					.toList());
		}

		return output;
	}

	@PostMapping("/rest/mail/selectedInbox")
	public String setDefaultInbox(@RequestParam(name = "inbox", required = false) String inbox, HttpSession httpSession) {
		final Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);
		if (!StringUtils.isEmpty(inbox) && constraints.contains(inbox)) {
			httpSession.setAttribute("selectedInbox", inbox);
		} else {
			httpSession.setAttribute("selectedInbox", constraints.stream().findFirst().orElseThrow());
		}
		return "mailbox";
	}

	@PostMapping("/rest/mails/{id}/archive")
	public ResponseEntity<?> archiveMail(@PathVariable long id, @RequestParam boolean archive) {
		Mail mail = mailService.getById(id);
		if (mail == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!applyArchive(mail, archive)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/mails/{id}/delete")
	public ResponseEntity<?> deleteMail(@PathVariable long id, @RequestParam boolean delete) {
		Mail mail = mailService.getById(id);
		if (mail == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!applyDelete(mail, delete)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record BulkDeleteDTO(List<Long> ids, boolean delete) {}

	record BulkArchiveDTO(List<Long> ids, boolean archive) {}

	record BulkMoveDTO(List<Long> ids, long folderId, String newFolderName) {}

	@PostMapping("/rest/mails/bulk/delete")
	public ResponseEntity<?> bulkDeleteMail(@RequestBody BulkDeleteDTO dto) {
		if (dto.ids() == null || dto.ids().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		for (long id : dto.ids()) {
			Mail mail = mailService.getById(id);
			if (mail == null) {
				continue;
			}
			applyDelete(mail, dto.delete());
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/mails/bulk/archive")
	public ResponseEntity<?> bulkArchiveMail(@RequestBody BulkArchiveDTO dto) {
		if (dto.ids() == null || dto.ids().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		for (long id : dto.ids()) {
			Mail mail = mailService.getById(id);
			if (mail == null) {
				continue;
			}
			applyArchive(mail, dto.archive());
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private boolean applyDelete(Mail mail, boolean delete) {
		if (delete) {
			mail.setFolder(Folder.DELETED);
			mail.setDeletedDate(LocalDate.now());
			mailService.save(mail);
			auditLogService.log(AuditLogOperation.MAIL_DELETE, getDetails(mail));
			return true;
		} else if (mail.getFolder().equals(Folder.DELETED)) {
			mail.setFolder(mail.getOriginalFolder());
			mail.setDeletedDate(null);
			mailService.save(mail);
			auditLogService.log(AuditLogOperation.MAIL_UNDELETE, getDetails(mail));
			return true;
		}
		return false;
	}

	private boolean applyArchive(Mail mail, boolean archive) {
		if (archive && mail.getFolder().equals(Folder.INBOX)) {
			mail.setFolder(Folder.ARCHIVE);
			mail.setInboxFolder(null);
			mailService.save(mail);
			return true;
		} else if (!archive && mail.getFolder().equals(Folder.ARCHIVE)) {
			mail.setFolder(Folder.INBOX);
			mailService.save(mail);
			return true;
		}
		return false;
	}

	@PostMapping("/rest/mails/bulk/move")
	public ResponseEntity<?> bulkMoveMail(@RequestBody BulkMoveDTO dto) {
		if (dto.ids() == null || dto.ids().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		InboxFolder targetFolder = null;
		if (dto.folderId() == FOLDER_CREATE_ID) {
			if (dto.newFolderName() == null || dto.newFolderName().isEmpty() || dto.newFolderName().length() > 255) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			InboxFolder newFolder = new InboxFolder();
			newFolder.setName(dto.newFolderName());
			targetFolder = inboxFolderService.save(newFolder);
		} else if (dto.folderId() != FOLDER_INBOX_ID) {
			targetFolder = inboxFolderService.findById(dto.folderId());
			if (targetFolder == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		}

		for (long id : dto.ids()) {
			Mail mail = mailService.getById(id);
			if (mail == null) {
				continue;
			}
			mail.setInboxFolder(targetFolder);
			mailService.save(mail);
		}

		return new ResponseEntity<>(targetFolder != null ? targetFolder.getId() : 0L, HttpStatus.OK);
	}

	private String getDetails(Mail mail) {
		String details = "";
		if (mail.getOriginalFolder().equals(Folder.INBOX) && mail.getSender() != null) {
			details = "Afsender: " +
					mail.getSender().getShortOrganisationName();
		} else if (mail.getOriginalFolder().equals(Folder.SENT) && mail.getRecipient() != null) {
			details = "Modtager: " +
					mail.getRecipient().getShortOrganisationName();
		}
		details = details +
				"\n" +
				"Titel: " +
				mail.getSubject();
		return details;
	}

	@PostMapping("/rest/mails/{id}/attachment/add")
	public ResponseEntity<?> uploadAttachment(@ModelAttribute AttachemntDTO attachment, @PathVariable long id) throws IOException {
		final Mail draft = mailService.getById(id);
		if (draft == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		if (draft.getReferences().size() >= 10) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der må maks uploades 10 vedhæftninger.");
		}

		if (draft.getBinaryMessage() != null) {
			long totalSize = draft.getBinaryMessage().getBinaries().stream()
					.mapToLong(Binary::getOriginalSize)
					.sum();
			if (totalSize + attachment.file.getSize() > 100000000) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Størrelsen på de vedhæftede filer overskrider maks (100MB)");
			}
		}

		String filename = attachment.file().getOriginalFilename();
		if (StringUtils.isEmpty(filename)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen fil uploadet");
		}
		final String cleanedFilename = filename.replace(".jpg", ".jpeg"); // for some reason jpg extension is not supported ... so rename to jpeg
		for (final Reference att : draft.getReferences()) {
			if (att.getFilename().equals(filename)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der findes allerede en fil med det navn");
			}
		}

		final InputStream is = attachment.file().getInputStream();
		final byte[] content = IOUtils.toByteArray(is);
		if (content == null || content.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen fil uploadet");
		}

		final ObjectExtensionCodeType objectExtensionCodeType = Arrays.stream(ObjectExtensionCodeType.values())
				.filter(t -> {
					if (StringUtils.endsWithIgnoreCase(cleanedFilename, t.value())) {
						return true;
					}
					if (t.value().equals(ObjectExtensionCodeType.JPEG.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "jpg")) {
						return true;
					}
					return t.value().equals(ObjectExtensionCodeType.TIFF.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "tif");
				})
				.findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Denne filtype supporteres ikke."));

		if (draft.getBinaryMessage() == null) {
			final BinaryMessage message = new BinaryMessage();
			message.setIncomming(false);
			final BinaryMessage savedMessage = binaryMessageService.save(message);
			draft.setBinaryMessage(savedMessage);
		}

		final String s3key = s3Service.upload(configuration.getS3().getBinDirectory(), filename, content);
		final String binIdentifier = UUID.randomUUID().toString();
		final Binary binary = new Binary();
		binary.setExtensionCode(objectExtensionCodeType.value());
		binary.setCode(emessageMapper.extensionToCode(objectExtensionCodeType).value());
		binary.setOriginalSize(attachment.file().getSize());
		binary.setIdentifier(binIdentifier);
		binary.setS3FileKey(s3key);
		final Binary savedBinary = binaryService.save(binary);
		savedBinary.setMessage(draft.getBinaryMessage());
		draft.getBinaryMessage().getBinaries().add(savedBinary);

		final Reference newAttachment = new Reference();
		newAttachment.setFilename(filename);
		newAttachment.setMail(draft);
		newAttachment.setReferenceType(ReferenceType.BIN);
		newAttachment.setObjectIdentifier(removeDashes(binIdentifier));
		newAttachment.setObjectExtensionCode(objectExtensionCodeType.value());
		newAttachment.setObjectOriginalSize(attachment.file().getSize());

		draft.getReferences().add(newAttachment);
		final Mail savedMail = mailService.save(draft);
		return new ResponseEntity<>(savedMail.getReferences().stream()
				.filter(r -> Objects.equals(r.getObjectIdentifier(), newAttachment.getObjectIdentifier()))
				.map(Reference::getId)
				.findFirst().orElse(0L), HttpStatus.OK);

	}

	public record PreloadedAttachmentDTO(String fileName, String content) {}

	@PostMapping("/rest/mails/{id}/attachment/add-preloaded")
	public ResponseEntity<?> addPreloadedAttachment(
			@PathVariable long id,
			@RequestBody PreloadedAttachmentDTO attachmentDTO) throws IOException {

		final Mail draft = mailService.getById(id);
		if (draft == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		if (draft.getReferences().size() >= 10) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der må maks uploades 10 vedhæftninger.");
		}

		if (attachmentDTO.content() == null || attachmentDTO.content().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fil indhold mangler");
		}

		// Decode base64 content
		byte[] content = Base64.getDecoder().decode(attachmentDTO.content());

		if (draft.getBinaryMessage() != null) {
			long totalSize = draft.getBinaryMessage().getBinaries().stream()
					.mapToLong(Binary::getOriginalSize)
					.sum();
			if (totalSize + content.length > 100000000) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Størrelsen på de vedhæftede filer overskrider maks (100MB)");
			}
		}

		String filename = attachmentDTO.fileName();
		if (StringUtils.isEmpty(filename)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen fil uploadet");
		}

		final String cleanedFilename = filename.replace(".jpg", ".jpeg");
		for (final Reference att : draft.getReferences()) {
			if (att.getFilename().equals(filename)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der findes allerede en fil med det navn");
			}
		}

		if (content == null || content.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen fil uploadet");
		}

		final ObjectExtensionCodeType objectExtensionCodeType = Arrays.stream(ObjectExtensionCodeType.values())
				.filter(t -> {
					if (StringUtils.endsWithIgnoreCase(cleanedFilename, t.value())) {
						return true;
					}
					if (t.value().equals(ObjectExtensionCodeType.JPEG.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "jpg")) {
						return true;
					}
					return t.value().equals(ObjectExtensionCodeType.TIFF.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "tif");
				})
				.findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Denne filtype supporteres ikke."));

		if (draft.getBinaryMessage() == null) {
			final BinaryMessage message = new BinaryMessage();
			message.setIncomming(false);
			final BinaryMessage savedMessage = binaryMessageService.save(message);
			draft.setBinaryMessage(savedMessage);
		}

		final String s3key = s3Service.upload(configuration.getS3().getBinDirectory(), filename, content);
		final String binIdentifier = UUID.randomUUID().toString();
		final Binary binary = new Binary();
		binary.setExtensionCode(objectExtensionCodeType.value());
		binary.setCode(emessageMapper.extensionToCode(objectExtensionCodeType).value());
		binary.setOriginalSize((long) content.length);
		binary.setIdentifier(binIdentifier);
		binary.setS3FileKey(s3key);
		final Binary savedBinary = binaryService.save(binary);
		savedBinary.setMessage(draft.getBinaryMessage());
		draft.getBinaryMessage().getBinaries().add(savedBinary);

		final Reference newAttachment = new Reference();
		newAttachment.setFilename(filename);
		newAttachment.setMail(draft);
		newAttachment.setReferenceType(ReferenceType.BIN);
		newAttachment.setObjectIdentifier(removeDashes(binIdentifier));
		newAttachment.setObjectExtensionCode(objectExtensionCodeType.value());
		newAttachment.setObjectOriginalSize((long) content.length);

		draft.getReferences().add(newAttachment);
		final Mail savedMail = mailService.save(draft);

		return new ResponseEntity<>(savedMail.getReferences().stream()
				.filter(r -> Objects.equals(r.getObjectIdentifier(), newAttachment.getObjectIdentifier()))
				.map(Reference::getId)
				.findFirst().orElse(0L), HttpStatus.OK);
	}

	@PostMapping("/rest/mails/{id}/attachment/{attachmentId}/delete")
	public ResponseEntity<?> deleteAttachment(@PathVariable long id, @PathVariable long attachmentId) {
		final Mail draft = mailService.getById(id);
		if (draft == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		for (Iterator<Reference> iterator = draft.getReferences().iterator(); iterator.hasNext(); ) {
			Reference attachment = iterator.next();

			if (attachment.getId() == attachmentId) {
				iterator.remove();
				binaryService.findByIdentifier(attachment.getObjectIdentifier())
						.ifPresent(b -> binaryService.delete(b));
				break;
			}
		}
		mailService.save(draft);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/mails/{id}/send")
	public ResponseEntity<?> sendMail(@RequestBody MailDTO dto, @PathVariable long id) {
		Mail draft = mailService.getById(id);
		if (!dto.answer && (dto.recipientEan == null || dto.recipientEan.isEmpty())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en modtager");
		}
		Recipient recipient = recipientService.findByEanIdentifier(dto.recipientEan).orElse(null);
		if (draft == null || (!dto.answer && recipient == null)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (dto.answer && recipient == null && draft.getAnswerTo() != null) {
			recipient = draft.getAnswerTo().getSender();
		}

		Sender sender = null;
		if (dto.answer()) {
			final String associatedIdentifier = draft.getAssociatedIdentifier();
			sender = configuration.getSenders().stream().filter(s -> s.getIdentifier().equals(associatedIdentifier)).findAny().orElse(null);
		} else {
			sender = configuration.getSenders().stream().filter(s -> s.getIdentifier().equals(dto.senderIdentifier)).findAny().orElse(null);
		}

		if (sender == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en afsender");
		}

		if (!dto.answer && dto.subject.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beskeden skal have et emne");
		}

		if (dto.content.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beskeden skal have noget indhold");
		}

		if (containsInvalidUtf8Mb3Chars(dto.content)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Beskeden indeholder ugyldige tegn (f.eks. emojis). Fjern venligst disse tegn og prøv igen.");
		}

		if (!dto.answer && containsInvalidUtf8Mb3Chars(dto.subject)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Emnet indeholder ugyldige tegn (f.eks. emojis). Fjern venligst disse tegn og prøv igen.");
		}

		if (dto.content.replace("\n", "<Break/>").length() > 25000) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beskeden er for lang. Den må maks være 25000 tegn");
		}

		if (!dto.answer && dto.subject.length() > 70) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emnet er for langt. Det må maks være 70 tegn");
		}

		if (!dto.answer) {
			String[] split = dto.patientName.split(" ");
			String surname = split[split.length - 1];
			String firstNames = dto.patientName.replace(" " + surname, "");
			if (surname.length() > 70 || firstNames.length() > 70) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patientens navn er for langt");
			}

			if (!validCpr(dto.patientCpr)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patientens cpr er ikke gyldigt. Det skal være 10 tegn og der må kun anvendes tal.");
			}
		}

		String content = dto.content;
		boolean answerTooLong = false;
		if (draft.getAnswerTo() != null) {
			String tryContent = content + "\n______________________________\nSvar til: \n" + draft.getAnswerTo().getContent();

			if (tryContent.replace("\n", "<Break/>").length() > 25000) {
				answerTooLong = true;
			} else {
				content = tryContent;
			}
		}


		LocalDateTime now = LocalDateTime.now();

		draft.setDraft(false);
		draft.setContent(content);
		draft.setSent(now);
		draft.setHighPriority(dto.highPriority);
		draft.setCaseId(dto.caseId);

		if (!dto.answer) {
			draft.setRecipient(recipient);
			draft.setSubject(dto.subject);

			Patient patient = new Patient();
			patient.setName(dto.patientName);
			patient.setCpr(dto.patientCpr());
			draft.setPatient(patient);
			draft.getPatient().setEpisodeOfCareStatusCode(EpisodeOfCareStatusCode.valueOf(dto.patientStatus));
		}

		senderService.sendMessage(draft, sender);
		if (draft.getBinaryMessage() != null) {
			senderService.sendBinaryMessage(draft, recipient, sender);
		}
		draft.setStatus(Status.WAITING);
		draft.setAssociatedIdentifier(sender.getIdentifier());
		draft = mailService.save(draft);

		String details = "Modtager: " +
				findRecipientName(recipient, draft) +
				"Titel: " +
				draft.getSubject();
		auditLogService.log(AuditLogOperation.MAIL_SENT, details);

		if (answerTooLong) {
			return new ResponseEntity<>("Tidligere korrespondance sendes ikke med i bunden af meddelelsesteksten, da den maksimale længde er overskredet. ", HttpStatus.OK);
		}

		return new ResponseEntity<>(userSession.getLandingInfo() == null ? draft.getId() : null, HttpStatus.OK);
	}

	private static byte[] getXmlBytes(final String xml) {
		return xml.getBytes(StandardCharsets.ISO_8859_1);
	}

	private static String findRecipientName(final Recipient recipient, final Mail draft) {
		return recipient != null ? recipient.getShortOrganisationName()
				: draft.getRecipient().getShortOrganisationName();
	}

	record MoveMailDTO(long folderId, String newFolderName) {
	}

	@PostMapping("/rest/mails/{id}/move")
	public ResponseEntity<?> sendMail(@RequestBody MoveMailDTO dto, @PathVariable long id) {
		Mail mail = mailService.getById(id);
		if (mail == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		long inboxFolderId = 0;
		if (dto.folderId() == FOLDER_CREATE_ID) {
			if (dto.newFolderName() == null || dto.newFolderName().isEmpty() || dto.newFolderName().length() > 255) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			InboxFolder inboxFolder = new InboxFolder();
			inboxFolder.setName(dto.newFolderName());
			inboxFolder = inboxFolderService.save(inboxFolder);
			mail.setInboxFolder(inboxFolder);
			inboxFolderId = inboxFolder.getId();
		} else if (dto.folderId() == FOLDER_INBOX_ID) {
			mail.setInboxFolder(null);
		} else {
			InboxFolder inboxFolder = inboxFolderService.findById(dto.folderId());
			if (inboxFolder == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			mail.setInboxFolder(inboxFolder);
			inboxFolderId = inboxFolder.getId();
		}

		mailService.save(mail);
		return new ResponseEntity<>(inboxFolderId, HttpStatus.OK);
	}

	private boolean validCpr(String cpr) {
		if (cpr == null || cpr.length() != 10) {
			return false;
		}

		for (char c : cpr.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}

		int days = Integer.parseInt(cpr.substring(0, 2));
		int month = Integer.parseInt(cpr.substring(2, 4));

		if (days < 1 || days > 31) {
			return false;
		}

		if (month < 1 || month > 12) {
			return false;
		}

		return true;
	}

	@GetMapping("/rest/mail/cprlookup/{cpr}")
	public String getNameFromCpr(@PathVariable String cpr) {
		CprLookupDto dto = iCprService.cprLookup(cpr);
		return dto != null
				? dto.getName() + " " + dto.getSurname()
				: "";
	}

	public record TemplateDto(String name, String subject, boolean isHighPriority, String content) {
	}

	@PostMapping("/rest/mail/template/save")
	public ResponseEntity<?> saveTemplate(@RequestBody TemplateDto templateDto) {

		if (templateDto.name == null || templateDto.name.isEmpty()) {
			throw new IllegalArgumentException("template has no name");
		}

		messageTemplateService.save(MessageTemplate.builder()
				.templateName(templateDto.name)
				.subject(templateDto.subject)
				.isHighPriority(templateDto.isHighPriority)
				.content(templateDto.content)
				.build());

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/rest/mail/template/{templateId}")
	public ResponseEntity<?> getNameFromCpr(@PathVariable long templateId) {
		record MessageTemplateDto(long id, String subject, boolean isHighPriority, String content) {
		}
		MessageTemplate template = messageTemplateService.get(templateId)
				.orElseThrow();

		return new ResponseEntity<>(
				new MessageTemplateDto(template.getId(),
						template.getSubject(),
						template.isHighPriority(),
						template.getContent()),
				HttpStatus.OK);
	}

	@GetMapping("/rest/mails/{id}/downloadAsPdf")
	public void downloadAsPdf(@PathVariable long id, HttpServletResponse response) throws IOException {
		Mail mail = mailService.getById(id);
		if (mail == null) {
			throw new IllegalArgumentException("Mail not found.");
		}

		String html = mailService.getPdfHtml(mail);
		byte[] pdf = mailService.convertHtmlToPdf(html);
		
		String fileNameDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
	
		String filename = "mail_" + fileNameDateTime + ".pdf";

		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
		response.setContentType(MediaType.APPLICATION_PDF_VALUE);

		try (InputStream is = new java.io.ByteArrayInputStream(pdf)) {
			org.springframework.util.StreamUtils.copy(is, response.getOutputStream());
		}
	}

	@Transactional
	@ResponseBody
	@PostMapping("/rest/mail/settings/setAutoReply")
	public ResponseEntity<?> setAutoReply(@Valid @RequestBody AutoReplyForm form) {
		Set<String> allowed = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);
		if (!allowed.isEmpty() && !allowed.contains(form.inboxEan())) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		if (form.startDate() != null && form.endDate() != null && form.endDate().isBefore(form.startDate())) {
			return new ResponseEntity<>("Slutdato må ikke være før startdato", HttpStatus.BAD_REQUEST);
		}
		Inbox inbox = inboxSubscriberService.getOrCreateInbox(form.inboxEan());
		inbox.setAutoReplyEnabled(form.enabled());
		inbox.setAutoReplySubject(form.subject());
		inbox.setAutoReplyMessage(form.message());
		inbox.setAutoReplyStartDate(form.startDate());
		inbox.setAutoReplyEndDate(form.endDate());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private boolean containsInvalidUtf8Mb3Chars(String str) {
		if (str == null) {
			return false;
		}

		for (int i = 0; i < str.length(); i++) {
			int codePoint = str.codePointAt(i);
			// Characters outside the Basic Multilingual Plane require 4 bytes in UTF-8
			if (codePoint > 0xFFFF) {
				return true;
			}
			// Skip the low surrogate if this was a supplementary character
			if (Character.isSupplementaryCodePoint(codePoint)) {
				i++;
			}
		}
		return false;
	}

}