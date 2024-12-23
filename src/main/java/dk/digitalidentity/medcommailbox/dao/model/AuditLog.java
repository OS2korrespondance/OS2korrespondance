package dk.digitalidentity.medcommailbox.dao.model;

import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "auditlog")
@Getter
@Setter
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "tts")
	private LocalDateTime timestamp;

	@Column
	private String ip;

	@Column
	private String performedBy;

	@Enumerated(EnumType.STRING)
	@Column
	private AuditLogOperation operation;

	@Column
	private String details;
}