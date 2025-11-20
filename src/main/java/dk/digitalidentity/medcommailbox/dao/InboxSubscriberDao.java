package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.InboxSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxSubscriberDao extends JpaRepository<InboxSubscriber, Long> {

}
