package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.model.entity.Mail;
import dk.digitalidentity.medcommailbox.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ReceiptController {

	private final MailService mailService;

	private record MailDTO(String recipient, String sender, String subject, LocalDateTime sentDate) {}

	@GetMapping("/receipt/{id}")
	public String receipt(Model model, @PathVariable long id) {
		Mail mail = mailService.getById(id);

		if (mail == null) {
			return "redirect:/receipt";
		}

		MailDTO dto = new MailDTO(
				mail.getRecipient() != null ? mail.getRecipient().getFullOrganisationName() : "Ukendt modtager",
				mail.getSender() != null ? mail.getSender().getFullOrganisationName() : "Ukendt afsender",
				mail.getSubject()   != null ? mail.getSubject()                              : "Ingen emne",
				mail.getSent()      != null ? mail.getSent()                                 : null
		);

		model.addAttribute("message", dto);
		return "receipt";
	}

	@GetMapping("/receipt")
	public String receipt(Model model) {
		model.addAttribute("message", null);
		return "receipt";
	}

}
