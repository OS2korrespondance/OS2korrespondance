package dk.digitalidentity.medcommailbox.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.MailService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class DeleteDraftsTask {
	
	@Autowired
	private MedcomMailboxConfiguration config;
	
	@Autowired
	private MailService mailService;
	
	@Scheduled(cron = "0 0 2 * * ?") //everyday at 2am
//	@Scheduled(fixedDelay = 15 * 1000) //every 15 seconds
	public void deleteDrafts() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		log.info("Removing old mail drafts.");

 		mailService.cleanUp();
	}
}
