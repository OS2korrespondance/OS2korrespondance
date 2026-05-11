package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileDao extends JpaRepository<UserProfile, Long> {
	Optional<UserProfile> findByUserId(String userId);
}
