package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequireAdminAccess
public class AdminController {

	@Autowired
	private RecipientService recipientService;

	@GetMapping("/admin/recipients")
	public String getRecipients(Model model) {
		model.addAttribute("recipients", recipientService.getAll());
		return "admin/recipients-list";
	}
}
