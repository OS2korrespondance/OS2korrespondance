package dk.digitalidentity.medcommailbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.medcommailbox.dao.MonitoringStateDao;
import dk.digitalidentity.medcommailbox.dao.MonitoringSubscriberDao;
import dk.digitalidentity.medcommailbox.model.api.ErrorReportRequest;
import dk.digitalidentity.medcommailbox.model.api.IsAlivePathStatus;
import dk.digitalidentity.medcommailbox.model.api.IsAliveRequest;
import dk.digitalidentity.medcommailbox.model.entity.MonitoringState;
import dk.digitalidentity.medcommailbox.model.entity.MonitoringSubscriber;
import dk.digitalidentity.medcommailbox.model.entity.enums.MonitoringSubscriberType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

	private final MonitoringStateDao monitoringStateDao;
	private final MonitoringSubscriberDao monitoringSubscriberDao;
	private final EmailService emailService;
	private final ObjectMapper objectMapper;

	// -- Error logs --
	@Transactional
	public void handleErrorReport(ErrorReportRequest request) {
		log.warn("Error reported from MedcomFileSync job '{}': {}", request.jobName(), request.errorMessage());

		MonitoringState state = getOrCreateState();
		String errorKey = request.jobName() + ":" + request.errorMessage().hashCode();

		boolean throttled = Objects.equals(errorKey, state.getLastErrorAlarmKey())
				&& state.getLastErrorAlarmSent() != null
				&& state.getLastErrorAlarmSent().isAfter(
				LocalDateTime.now().minusMinutes(state.getAlarmIntervalMinutes()));

		if (throttled) {
			log.debug("Error alarm throttled for key '{}'", errorKey);
			return;
		}

		sendAlarm("Fejl i MedcomFileSync (" + request.jobName() + "):<br/>" + request.errorMessage());
		state.setLastErrorAlarmKey(errorKey);
		state.setLastErrorAlarmSent(LocalDateTime.now());
		monitoringStateDao.save(state);
	}

	// -- IsAlive registration (called by C# client) --

	@Transactional
	public void registerIsAlive(IsAliveRequest request) {
		MonitoringState state = getOrCreateState();
		List<String> alarms = new ArrayList<>();

		// Detect restart: uptime decreased means the service was restarted
		if (state.getUptimeSeconds() != null && request.getUptimeSeconds() < state.getUptimeSeconds()) {
			log.warn("MedcomFileSync restart detected (uptime dropped from {}s to {}s)",
					state.getUptimeSeconds(), request.getUptimeSeconds());
			alarms.add("MedcomFileSync er blevet genstartet (oppetid faldt fra "
					+ formatUptime(state.getUptimeSeconds()) + " til "
					+ formatUptime(request.getUptimeSeconds()) + ")");
		}

		// Detect path changes (compare only name + directoryPath, not accessible)
		String newPathStructure = toPathStructureJson(request.getPaths());
		String previousPathStructure = toPathStructureJson(fromJson(state.getPathsJson()));
		if (state.getPathsJson() != null && !Objects.equals(previousPathStructure, newPathStructure)) {
			String changeDetails = buildPathChangeDetails(fromJson(state.getPathsJson()), request.getPaths());
			log.warn("MedcomFileSync path configuration changed: {}", changeDetails);
			alarms.add("Mappestier er ændret i MedcomFileSync konfigurationen:<br/>" + changeDetails);
		}

		// Detect inaccessible paths
		List<String> inaccessiblePaths = request.getPaths().stream()
				.filter(p -> !p.isAccessible())
				.map(p -> p.getName() + " (" + p.getDirectoryPath() + ")")
				.toList();
		if (!inaccessiblePaths.isEmpty()) {
			log.warn("MedcomFileSync has inaccessible paths: {}", inaccessiblePaths);
			alarms.add("Manglende adgang til mapper: " + String.join(", ", inaccessiblePaths));
		}

		// Reset alarm throttle when service comes back after being down
		if (state.getLastSeen() != null
				&& state.getLastSeen().isBefore(LocalDateTime.now().minusMinutes(15))) {
			log.info("MedcomFileSync is back online after being down since {}", state.getLastSeen());
			state.setLastAlarmSent(null);
		}

		// Update state
		state.setPreviousUptimeSeconds(state.getUptimeSeconds());
		state.setUptimeSeconds(request.getUptimeSeconds());
		state.setPreviousPathsJson(state.getPathsJson());
		state.setPathsJson(toJson(request.getPaths()));
		state.setLastSeen(LocalDateTime.now());
		monitoringStateDao.save(state);

		// Send one combined alarm if any issues detected
		if (!alarms.isEmpty()) {
			sendAlarm(String.join("<br/><br/>", alarms));
		}
	}

	// -- Scheduled check: is the service still alive? --

	@Transactional
	public void checkIsAlive() {
		MonitoringState state = getOrCreateState();
		if (state.getLastSeen() == null) {
			return;
		}

		boolean isDown = state.getLastSeen().isBefore(LocalDateTime.now().minusMinutes(15));
		if (!isDown) {
			return;
		}

		boolean alarmThrottled = state.getLastAlarmSent() != null
				&& state.getLastAlarmSent().isAfter(
				LocalDateTime.now().minusMinutes(state.getAlarmIntervalMinutes()));
		if (alarmThrottled) {
			return;
		}

		log.warn("MedcomFileSync has not reported in over 15 minutes (last seen: {})", state.getLastSeen());
		sendAlarm("MedcomFileSync har ikke rapporteret siden " + state.getLastSeen().toString().replace("T", " "));
		state.setLastAlarmSent(LocalDateTime.now());
		monitoringStateDao.save(state);
	}

	// -- Subscriber management --

	public List<MonitoringSubscriber> getSubscribers(MonitoringSubscriberType type) {
		return monitoringSubscriberDao.findBySubscriberType(type);
	}

	@Transactional
	public void addSubscriber(String email, MonitoringSubscriberType type) {
		if (monitoringSubscriberDao.findByEmailAndSubscriberType(email, type).isPresent()) {
			return;
		}
		MonitoringSubscriber subscriber = new MonitoringSubscriber();
		subscriber.setEmail(email);
		subscriber.setSubscriberType(type);
		monitoringSubscriberDao.save(subscriber);
	}

	@Transactional
	public void removeSubscriber(String email, MonitoringSubscriberType type) {
		monitoringSubscriberDao.findByEmailAndSubscriberType(email, type)
				.ifPresent(monitoringSubscriberDao::delete);
	}

	// -- Alarm interval --

	public MonitoringState getOrCreateState() {
		return monitoringStateDao.findById(1L).orElseGet(() -> {
			MonitoringState state = new MonitoringState();
			return monitoringStateDao.save(state);
		});
	}

	@Transactional
	public void setAlarmInterval(int minutes) {
		MonitoringState state = getOrCreateState();
		state.setAlarmIntervalMinutes(minutes);
		monitoringStateDao.save(state);
	}

	// -- Internal helpers --

	private void sendAlarm(String message) {
		List<MonitoringSubscriber> subscribers = monitoringSubscriberDao
				.findBySubscriberType(MonitoringSubscriberType.AUTOMATIC_ALARM);

		if (subscribers.isEmpty()) {
			log.warn("Monitoring alarm triggered but no subscribers configured: {}", message);
			return;
		}

		String subject = "OS2korrespondance - Overvågningsalarm";
		for (MonitoringSubscriber subscriber : subscribers) {
			boolean sent = emailService.sendMessage(subscriber.getEmail(), subject, message);
			if (sent) {
				log.info("Monitoring alarm sent to {}: {}", subscriber.getEmail(), message);
			}
		}
	}

	private String toJson(List<IsAlivePathStatus> paths) {
		try {
			return objectMapper.writeValueAsString(paths);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize paths to JSON", e);
			return "[]";
		}
	}

	private String formatUptime(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		if (hours > 0) {
			return hours + "t " + minutes + "m";
		}
		return minutes + "m";
	}

	private String toPathStructureJson(List<IsAlivePathStatus> paths) {
		if (paths == null || paths.isEmpty()) {
			return "[]";
		}
		try {
			List<String> structures = paths.stream()
					.map(p -> p.getName() + "|" + p.getDirectoryPath())
					.sorted()
					.toList();
			return objectMapper.writeValueAsString(structures);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize path structure to JSON", e);
			return "[]";
		}
	}

	private List<IsAlivePathStatus> fromJson(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize paths from JSON", e);
			return List.of();
		}
	}

	private String buildPathChangeDetails(List<IsAlivePathStatus> oldPaths, List<IsAlivePathStatus> newPaths) {
		Map<String, String> oldMap = oldPaths.stream()
				.collect(Collectors.toMap(IsAlivePathStatus::getName, IsAlivePathStatus::getDirectoryPath));
		Map<String, String> newMap = newPaths.stream()
				.collect(Collectors.toMap(IsAlivePathStatus::getName, IsAlivePathStatus::getDirectoryPath));

		List<String> changes = new ArrayList<>();

		// Paths removed
		for (Map.Entry<String, String> entry : oldMap.entrySet()) {
			if (!newMap.containsKey(entry.getKey())) {
				changes.add("Fjernet: " + entry.getKey() + " (" + entry.getValue() + ")");
			}
		}

		// Paths added
		for (Map.Entry<String, String> entry : newMap.entrySet()) {
			if (!oldMap.containsKey(entry.getKey())) {
				changes.add("Tilføjet: " + entry.getKey() + " (" + entry.getValue() + ")");
			}
		}

		// Paths with changed directory
		for (Map.Entry<String, String> entry : newMap.entrySet()) {
			String oldPath = oldMap.get(entry.getKey());
			if (oldPath != null && !oldPath.equals(entry.getValue())) {
				changes.add(entry.getKey() + " ændret fra " + oldPath + " til " + entry.getValue());
			}
		}

		return String.join("<br/>", changes);
	}
}
