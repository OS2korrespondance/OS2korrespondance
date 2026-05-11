package dk.digitalidentity.medcommailbox.model.entity;

import dk.digitalidentity.medcommailbox.model.entity.enums.MonitoringSubscriberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "monitoring_subscriber")
public class MonitoringSubscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "subscriber_type", nullable = false)
	private MonitoringSubscriberType subscriberType;
}
