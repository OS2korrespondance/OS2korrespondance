package dk.digitalidentity.medcommailbox.dao;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.medcommailbox.dao.model.AuditLog;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {
	AuditLog findById(long id);

	long deleteByTimestampBefore(LocalDateTime timestamp);
}
