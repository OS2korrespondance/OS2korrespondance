package dk.digitalidentity.medcommailbox.controller.mvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.dao.model.InboxFolder;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.InboxFolderService;
import dk.digitalidentity.medcommailbox.service.MedcomLogService;
import dk.digitalidentity.medcommailbox.session.LandingInfo;
import dk.digitalidentity.medcommailbox.session.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.dao.model.enums.Status;
import dk.digitalidentity.medcommailbox.service.MailService;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import org.springframework.web.bind.annotation.RequestParam;

import static dk.digitalidentity.medcommailbox.Constants.FOLDER_INBOX_ID;
import static dk.digitalidentity.medcommailbox.util.NullSafe.nullSafe;

@RequireUserAccess
@Controller
public class MailController {

	@Autowired
	private MailService mailService;

	@Autowired
	private RecipientService recipientService;

	@Autowired
	private MedcomMailboxConfiguration configuration;

	@Autowired
	private MedcomLogService medcomLogService;

	@Autowired
	private InboxFolderService inboxFolderService;

	@Autowired
	private UserSession userSession;
	
	@GetMapping("/mailbox/{folder}")
	public String getMailbox(Model model, @PathVariable Folder folder, @RequestParam(name = "folder", required = false) Long inboxFolderId) {
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName());
		if (constraints == null) {
			return "redirect:/error";
		}

		List<Mail> mails = mailService.getByFolder(folder);
		InboxFolder inboxFolder = null;
		if (folder.equals(Folder.INBOX)) {
			if (inboxFolderId != null) {
				inboxFolder = inboxFolderService.findById(inboxFolderId);
			}

			if (inboxFolder != null) {
				mails = mails.stream().filter(m -> m.getInboxFolder() != null && m.getInboxFolder().getId() == inboxFolderId).collect(Collectors.toList());
			} else {
				mails = mails.stream().filter(m -> m.getInboxFolder() == null).collect(Collectors.toList());
			}
		}

		if (!constraints.isEmpty()) {
			mails = mails.stream().filter(m -> constraints.contains(m.getAssociatedIdentifier())).collect(Collectors.toList());
		}

		model.addAttribute("mails", mails);
		model.addAttribute("inboxUnreadCount", mailService.getCountByReadAndFolder(false, Folder.INBOX));
		model.addAttribute("negativeReceiptCount", mailService.getCountByFolderAndReceivedNegativeReceipt(Folder.SENT));
		model.addAttribute("folderCount", mails.size());
		model.addAttribute("folder", folder);
		model.addAttribute("inboxFolder", inboxFolder);
		model.addAttribute("inboxFolders", inboxFolderService.findAll());

		return "mailbox";		
	}

	@GetMapping("/mail/{id}")
	public String getMail(Model model, @PathVariable long id) {
		Mail mail = mailService.getById(id);
		if (mail == null) {
			return "redirect:/mailbox/INBOX";
		}

		if (!mail.isRead()) {
			mail.setRead(true);
			mail = mailService.save(mail);
		}

		model.addAttribute("inboxUnreadCount", mailService.getCountByReadAndFolder(false, Folder.INBOX));
		model.addAttribute("mail", mail);
		model.addAttribute("content", mail.getContent().replace("\n", "<br/>"));
		model.addAttribute("daysBeforeDeletion", configuration.getDaysBeforeDeletion());
		model.addAttribute("log", medcomLogService.getByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(mail.getEnvelopeIdentifier(), mail.getLetterIdentifier()).orElse(null));
		if (mail.getBinaryMessage() != null) {
			model.addAttribute("binlog", medcomLogService.getByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(mail.getBinaryMessage().getEnvelopeIdentifier(), mail.getBinaryMessage().getLetterIdentifier()).orElse(null));
		}
		model.addAttribute("inboxFolders", inboxFolderService.findAll());
		model.addAttribute("inboxFolderId", mail.getInboxFolder() == null ? FOLDER_INBOX_ID : mail.getInboxFolder().getId());

		return "view_mail";		
	}
	
	@GetMapping("/mail/{id}/answer")
	public String answer(Model model, @PathVariable long id) {
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName());
		if (constraints == null) {
			return "redirect:/error";
		}

		Mail mail = mailService.getById(id);
		if (mail == null) {
			return "redirect:/mailbox/INBOX";
		}

		List<Sender> senders = configuration.getSenders();
		if (!constraints.isEmpty()) {
			senders = senders.stream().filter(s -> constraints.contains(s.getIdentifier())).collect(Collectors.toList());
		}
		
		Mail newMail = createDraftMail(mail);
		
		model.addAttribute("inboxUnreadCount", mailService.getCountByReadAndFolder(false, Folder.INBOX));
		model.addAttribute("negativeReceiptCount", mailService.getCountByFolderAndReceivedNegativeReceipt(Folder.SENT));
		model.addAttribute("id", newMail.getId());
		model.addAttribute("answer", true);
		model.addAttribute("patient", newMail.getPatient());
		model.addAttribute("subject", mail.getSubject());
		model.addAttribute("to", newMail.getRecipient().getShortOrganisationName());

		model.addAttribute("senders", senders);

		return "compose_mail";		
	}
	
	@GetMapping("/mail/new")
	public String newMail(Model model) {
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName());
		if (constraints == null) {
			return "redirect:/error";
		}

		Mail newMail = new Mail();
		newMail.setDraft(true);
		newMail.setContent("");
		newMail.setFolder(Folder.SENT);
		newMail.setOriginalFolder(Folder.SENT);
		newMail.setSubject("");
		newMail.setRead(true);
		//TODO sæt rigtig status og afsender, når vi ved hvad der skal bruges
		newMail.setStatus(Status.WAITING);
		newMail = mailService.save(newMail);

		final List<Sender> senders = configuration.getSenders().stream()
				.filter(s -> constraints.isEmpty() || constraints.contains(s.getIdentifier()))
				.toList();

		final List<Recipient> allRecipients = recipientService.getAll();
		model.addAttribute("inboxUnreadCount", mailService.getCountByReadAndFolder(false, Folder.INBOX));
		model.addAttribute("negativeReceiptCount", mailService.getCountByFolderAndReceivedNegativeReceipt(Folder.SENT));
		model.addAttribute("id", newMail.getId());
		model.addAttribute("answer", false);
		model.addAttribute("recipients", allRecipients);
		model.addAttribute("selectedRecipient", nullSafe(() -> allRecipients.stream().filter(r -> userSession.getLandingInfo().getReceiverEan().equals(r.getEanIdentifier())).findFirst().orElse(null), null));
		model.addAttribute("senders", senders);
		model.addAttribute("selectedSender", nullSafe(() -> senders.stream().filter(s -> userSession.getLandingInfo().getSenderEan().equals(s.getEanIdentifier())).findFirst().orElse(null), null));
		model.addAttribute("patientName", nullSafe(() -> userSession.getLandingInfo().getPatientName(), ""));
		model.addAttribute("patientCpr", nullSafe(() -> userSession.getLandingInfo().getPatientCpr(), ""));
		model.addAttribute("subject", nullSafe(() -> userSession.getLandingInfo().getSubject(), ""));
		model.addAttribute("selectedStatus", nullSafe(() -> userSession.getLandingInfo().getEpisodeOfCareStatusCode(), null));
		model.addAttribute("caseId", nullSafe(() -> userSession.getLandingInfo().getCaseId(), null));
		userSession.setLandingInfo(LandingInfo.builder().build());
		return "compose_mail";
	}

	private Mail createDraftMail(Mail mail) {
		Mail newMail = new Mail();
		newMail.setDraft(true);
		newMail.setContent("");
		newMail.setFolder(Folder.SENT);
		newMail.setOriginalFolder(Folder.SENT);
		newMail.setRecipient(mail.getSender());
		newMail.setSubject(mail.getSubject());
		newMail.setRead(true);
		//TODO sæt rigtig status, når vi ved hvad der skal bruges
		newMail.setStatus(Status.WAITING);
		newMail.setAnswerTo(mail);
		newMail.setPatient(mail.getPatient());
		newMail.setAssociatedIdentifier(mail.getAssociatedIdentifier());
		return mailService.save(newMail);
	}
}
