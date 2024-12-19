package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.dao.model.AuditLog;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequireAdminAccess
@Controller
public class AuditLogController {

	@Autowired
	private AuditLogService auditLogService;

	@GetMapping("/auditlog")
	public String log(Model model) {
		model.addAttribute("operations", AuditLogOperation.getSorted());
		return "auditlog/log";
	}

	@GetMapping("/auditlog/{id}/details")
	public String logDetails(Model model, @PathVariable long id) {
		AuditLog log = auditLogService.getById(id);
		if (log == null) {
			return "redirect:/error";
		}

		model.addAttribute("log", log);

		return "auditlog/log_details";
	}
}
