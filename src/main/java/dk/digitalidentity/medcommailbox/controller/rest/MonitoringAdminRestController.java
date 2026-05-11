package dk.digitalidentity.medcommailbox.controller.rest;

import dk.digitalidentity.medcommailbox.model.entity.enums.MonitoringSubscriberType;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import dk.digitalidentity.medcommailbox.service.MonitoringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAdminAccess
@RestController
@RequestMapping("/rest/admin/monitoring")
@RequiredArgsConstructor
public class MonitoringAdminRestController {

	private final MonitoringService monitoringService;

	record MonitoringSubscriberForm(@NotNull MonitoringSubscriberType type, @NotEmpty String email) {}

	@PostMapping("/addSubscriber")
	public ResponseEntity<Void> addSubscriber(@Valid @RequestBody MonitoringSubscriberForm form) {
		monitoringService.addSubscriber(form.email(), form.type());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/removeSubscriber")
	public ResponseEntity<Void> removeSubscriber(@Valid @RequestBody MonitoringSubscriberForm form) {
		monitoringService.removeSubscriber(form.email(), form.type());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	record AlarmIntervalForm(@NotNull int minutes) {}

	@PostMapping("/setAlarmInterval")
	public ResponseEntity<Void> setAlarmInterval(@Valid @RequestBody AlarmIntervalForm form) {
		if (form.minutes() < 30) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		monitoringService.setAlarmInterval(form.minutes());
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
