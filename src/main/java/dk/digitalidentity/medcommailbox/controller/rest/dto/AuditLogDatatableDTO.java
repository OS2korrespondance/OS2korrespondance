package dk.digitalidentity.medcommailbox.controller.rest.dto;

import dk.digitalidentity.medcommailbox.datatables.model.AuditLogDatatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDatatableDTO {
	private long id;
	private LocalDateTime timestamp;
	private String performedBy;
	private String operation;

	public AuditLogDatatableDTO(AuditLogDatatable auditlog) {
		this.id = auditlog.getId();
		this.timestamp = auditlog.getTimestamp();
		this.performedBy = auditlog.getPerformedBy();
		this.operation = auditlog.getOperation().getMessage();
	}
}
