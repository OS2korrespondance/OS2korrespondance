package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.ApiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiMessageDao extends JpaRepository<ApiMessage, Long> {
	ApiMessage findByGroupId(String groupId);

    List<ApiMessage> findByCreatedAtBefore(LocalDateTime before);
}
