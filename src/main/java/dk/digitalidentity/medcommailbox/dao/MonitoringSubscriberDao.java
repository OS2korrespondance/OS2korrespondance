package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.MonitoringSubscriber;
import dk.digitalidentity.medcommailbox.model.entity.enums.MonitoringSubscriberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitoringSubscriberDao extends JpaRepository<MonitoringSubscriber, Long> {
	List<MonitoringSubscriber> findBySubscriberType(MonitoringSubscriberType subscriberType);
	Optional<MonitoringSubscriber> findByEmailAndSubscriberType(String email, MonitoringSubscriberType subscriberType);
}
