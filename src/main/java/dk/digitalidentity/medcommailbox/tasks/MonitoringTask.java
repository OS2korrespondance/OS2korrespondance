package dk.digitalidentity.medcommailbox.tasks;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.MonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class MonitoringTask {

	@Autowired
	private MedcomMailboxConfiguration config;

	@Autowired
	private MonitoringService monitoringService;

	@Scheduled(fixedRate = 300_000) // every 5 minutes
	public void checkIsAlive() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		monitoringService.checkIsAlive();
	}
}
