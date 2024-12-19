package dk.digitalidentity.medcommailbox.tasks;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class DeleteFromDeletedFolderTask {

	@Autowired
	private MedcomMailboxConfiguration config;

	@Autowired
	private MailService mailService;

	@Scheduled(cron = "0 0 3 * * ?") //everyday at 3am
//	@Scheduled(fixedDelay = 15 * 100000) // for testing
	public void deleteDrafts() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		log.info("Deleting mail from deleted folder.");

		mailService.deleteMails();
	}
}
