package dk.digitalidentity.medcommailbox.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "monitoring_state")
public class MonitoringState {

	@Id
	private Long id = 1L;

	@Column(name = "last_seen")
	private LocalDateTime lastSeen;

	@Column(name = "uptime_seconds")
	private Long uptimeSeconds;

	@Column(name = "previous_uptime_seconds")
	private Long previousUptimeSeconds;

	@Column(name = "paths_json", columnDefinition = "TEXT")
	private String pathsJson;

	@Column(name = "previous_paths_json", columnDefinition = "TEXT")
	private String previousPathsJson;

	@Column(name = "last_alarm_sent")
	private LocalDateTime lastAlarmSent;

	@Column(name = "alarm_interval_minutes", nullable = false)
	private int alarmIntervalMinutes = 180;

	@Column(name = "last_error_alarm_sent")
	private LocalDateTime lastErrorAlarmSent;

	@Column(name = "last_error_alarm_key")
	private String lastErrorAlarmKey;
}
