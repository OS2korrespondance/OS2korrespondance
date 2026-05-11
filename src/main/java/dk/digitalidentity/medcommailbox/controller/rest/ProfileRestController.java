package dk.digitalidentity.medcommailbox.controller.rest;

import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequireUserAccess
@RestController
@Slf4j
public class ProfileRestController {

	@Autowired
	private SecurityUtil securityUtil;

	@Autowired
	private UserProfileService userProfileService;

	record SignatureDTO(String signature) {}

	@PostMapping("/rest/profile/signature")
	public ResponseEntity<?> saveSignature(@RequestBody SignatureDTO dto) {
		String userId = securityUtil.getUserId();
		if (userId == null) {
			return ResponseEntity.status(401).build();
		}

		String signature = dto.signature() != null ? dto.signature() : "";
		userProfileService.save(userId, signature);

		log.info("User {} updated their email signature", userId);
		return ResponseEntity.ok().build();
	}
}
