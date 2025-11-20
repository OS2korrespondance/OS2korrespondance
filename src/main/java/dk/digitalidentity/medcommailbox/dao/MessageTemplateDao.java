package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateDao extends JpaRepository<MessageTemplate, Long> {
}
