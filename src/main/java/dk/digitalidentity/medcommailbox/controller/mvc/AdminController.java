package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.model.entity.Inbox;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import dk.digitalidentity.medcommailbox.service.InboxSubscriberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequireAdminAccess
@RequiredArgsConstructor
public class AdminController {
	private final MedcomMailboxConfiguration configuration;
	private final InboxSubscriberService subscriberService;
	private final RecipientService recipientService;

    public record InboxDTO(String eanIdentifier,
            String organisationName,
            boolean negativeReceiptNotification,
            boolean autoReplyEnabled,
            String autoReplySubject,
            String autoReplyMessage,
            LocalDate autoReplyStartDate,
            LocalDate autoReplyEndDate)
    {}

	@GetMapping("/admin/recipients")
	public String getRecipients(Model model) {
		model.addAttribute("recipients", recipientService.getAll());
		return "admin/recipients-list";
	}

	@GetMapping("/admin/settings")
	public String getSettings(Model model) {
		setInboxes(model);
		model.addAttribute("subscribers", subscriberService.mapOfSubscribers());
		return "admin/settings";
	}

	@GetMapping("/admin/settings/emailconfig")
	public String getEmailConfigurationFragment(Model model) {
		setInboxes(model);
		model.addAttribute("subscribers", subscriberService.mapOfSubscribers());
		return "admin/fragments/email :: email";
	}

	@GetMapping("/admin/settings/autoreplyconfig")
	public String getAutoReplyConfigurationFragment(Model model) {
		setInboxes(model);
		model.addAttribute("autoReplyUrl", "/rest/admin/settings/setAutoReply");
		return "admin/fragments/auto-reply :: autoReplyList";
	}

	private void setInboxes(Model model) {
		model.addAttribute("inboxes", configuration.getSenders().stream()
				.map(s -> {
					final Inbox inbox = subscriberService.getOrCreateInbox(s.getEanIdentifier());
					return new InboxDTO(s.getEanIdentifier(), s.getOrganisationName(), inbox.isNegativeReceiptNotification(),
						inbox.isAutoReplyEnabled(), inbox.getAutoReplySubject(), inbox.getAutoReplyMessage(),
						inbox.getAutoReplyStartDate(), inbox.getAutoReplyEndDate());
				}
				)
				.toList());
	}
}
