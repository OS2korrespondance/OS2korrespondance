package dk.digitalidentity.medcommailbox.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.medcommailbox.service.MedcomLogService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class RemoveOldLogsTask {

	@Autowired
	private MedcomMailboxConfiguration config;

	@Autowired
	private MedcomLogService medcomLogService;

	@Autowired
    private AuditLogService auditLogService;

	@Scheduled(cron = "${medcom-mailbox.log-delete-task-cron}")
	public void deleteOldLogs() {
		if (!config.isSchedulingEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		log.info("Deleting old logs.");
		
		medcomLogService.deleteOldLogs(config.getLogDeleteAfter());
		auditLogService.cleanUp(config.getLogDeleteAfter());
		
		log.info("Done deleting old logs.");
	}
}
