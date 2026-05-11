package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.UserProfileDao;
import dk.digitalidentity.medcommailbox.model.entity.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

	@Autowired
	private UserProfileDao userProfileRepository;

	public Optional<UserProfile> getByUserId(String userId) {
		return userProfileRepository.findByUserId(userId);
	}

	public String getSignature(String userId) {
		return userProfileRepository.findByUserId(userId)
				.map(UserProfile::getSignature)
				.orElse("");
	}

	public UserProfile save(String userId, String signature) {
		UserProfile profile = userProfileRepository.findByUserId(userId)
				.orElseGet(() -> {
					UserProfile newProfile = new UserProfile();
					newProfile.setUserId(userId);
					return newProfile;
				});
		profile.setSignature(signature);
		return userProfileRepository.save(profile);
	}
}
