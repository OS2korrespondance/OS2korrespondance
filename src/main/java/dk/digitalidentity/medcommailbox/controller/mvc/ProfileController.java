package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequireUserAccess
@Controller
public class ProfileController {

	@Autowired
	private SecurityUtil securityUtil;

	@Autowired
	private UserProfileService userProfileService;

	@GetMapping("/profile")
	public String getProfile(Model model) {
		String userId = securityUtil.getUserId();
		String signature = userProfileService.getSignature(userId);

		model.addAttribute("signature", signature);
		model.addAttribute("page", "profile");
		return "profile";
	}
}
