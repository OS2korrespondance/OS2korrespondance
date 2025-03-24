package dk.digitalidentity.medcommailbox.service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;

import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.dao.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.medcommailbox.dao.AuditLogDao;
import dk.digitalidentity.medcommailbox.dao.model.AuditLog;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import dk.digitalidentity.medcommailbox.util.DurationUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditLogService {
	
	@Autowired
	private AuditLogDao auditLogDao;
	@Autowired
	private SecurityUtil securityUtil;
	
	public AuditLog getById(long id) {
		return auditLogDao.findById(id);
	}

	public void logSystem(AuditLogOperation operation, String details) {
		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation(operation);
		auditLog.setDetails(details);
		auditLog.setPerformedBy("System");

		auditLogDao.save(auditLog);
	}

	public void log(AuditLogOperation operation, String details) {
		log(operation, details, null);
	}
	
	public void log(AuditLogOperation operation, String details, String username) {
		if (username == null) {
			username = securityUtil.getUserId();
		}

		if (username == null) {
			log.error("Would auditLog operation: \"" + operation + "\", but user was null.");
			return;
		}

		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation(operation);
		auditLog.setDetails(details);
		auditLog.setPerformedBy(username);

		auditLogDao.save(auditLog);
	}

	@Transactional
	public void cleanUp(String deleteAfter) {
		TemporalAmount amount = DurationUtil.parse(deleteAfter);
		LocalDateTime dateNow = LocalDateTime.now();
		LocalDateTime deleteBeforeDate = dateNow.minus(amount);

		auditLogDao.deleteByTimestampBefore(deleteBeforeDate);
	}

	public String getDetails(final Recipient recipient) {
		StringBuilder details = new StringBuilder();
		details.append("FullOrganizationName: " + recipient.getFullOrganisationName());
		details.append("\r\n");
		details.append("ShortOrganizationName: " + recipient.getShortOrganisationName());
		details.append("\r\n");
		details.append("Identifier: " + recipient.getIdentifier());
		details.append("\r\n");
		details.append("IdentifierCode: " + recipient.getIdentifierCode());
		details.append("\r\n");
		details.append("Ean: " + recipient.getEanIdentifier());
		return details.toString();
	}

	public String getDetails(final Reference reference, final String filename) {
		StringBuilder details = new StringBuilder();
		details.append("Filnavn: " + filename);
		details.append("\r\n");
		details.append("FilId: " + reference.getId());
		details.append("\r\n");
		details.append("Mail: " + reference.getMail().getSubject());
		return details.toString();
	}
}
