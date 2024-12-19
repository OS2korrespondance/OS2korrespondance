package dk.digitalidentity.medcommailbox.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.MedcomReceiveService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class UpdateInboxAndReceiptsTask {
	
	@Autowired
	private MedcomMailboxConfiguration config;
	
	@Autowired
	private MedcomReceiveService receiveService;
	
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void update() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		log.debug("Updating inbox and receipts.");
		receiveService.receive();

	}
}
