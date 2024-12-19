package dk.digitalidentity.medcommailbox.datatables;

import dk.digitalidentity.medcommailbox.datatables.model.AuditLogDatatable;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface AuditLogDatatableDao extends DataTablesRepository<AuditLogDatatable, Long> {

}
