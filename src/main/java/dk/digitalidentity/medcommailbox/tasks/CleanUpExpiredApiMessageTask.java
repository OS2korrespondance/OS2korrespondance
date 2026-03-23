package dk.digitalidentity.medcommailbox.tasks;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.ApiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class CleanUpExpiredApiMessageTask {
	
	private final ApiMessageService apiMessageService;
	private final MedcomMailboxConfiguration config;

	@Scheduled(cron = "0 0 * * * ?") // Every Hour
//	@Scheduled(fixedDelay = 15 * 1000) // Every 15 second (for test)
	public void deleteExpiredApiMessages() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		log.info("Starting task: deleteExpiredApiMessages.");

		apiMessageService.deleteExpiredMessaged();

		log.info("Finished task: deleteExpiredApiMessages.");
	}
}
