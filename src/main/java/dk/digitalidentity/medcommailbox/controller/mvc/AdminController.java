package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import dk.digitalidentity.medcommailbox.service.InboxSubscriberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequireAdminAccess
@RequiredArgsConstructor
public class AdminController {
	private final MedcomMailboxConfiguration configuration;
	private final InboxSubscriberService subscriberService;
	private final RecipientService recipientService;

    public record InboxDTO(String eanIdentifier,
            String organisationName,
            boolean negativeReceiptNotification)
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

	private void setInboxes(Model model) {
		model.addAttribute("inboxes", configuration.getSenders().stream()
				.map(s -> {
					final var inbox = subscriberService.getOrCreateInbox(s.getEanIdentifier());
					return new InboxDTO(s.getEanIdentifier(), s.getOrganisationName(), inbox.isNegativeReceiptNotification());
				}
				)
				.toList());
	}
}
