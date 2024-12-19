package dk.digitalidentity.medcommailbox.controller.rest;

import dk.digitalidentity.medcommailbox.controller.rest.dto.AuditLogDatatableDTO;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.datatables.AuditLogDatatableDao;
import dk.digitalidentity.medcommailbox.datatables.model.AuditLogDatatable;
import dk.digitalidentity.medcommailbox.security.RequireAdminAccess;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RequireAdminAccess
@RestController
public class AuditLogRestController {

	@Autowired
	private AuditLogDatatableDao auditLogDatatableDao;

	@PostMapping("/rest/auditlog/list")
	public DataTablesOutput<AuditLogDatatableDTO> adminEventLogsDataTable(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<AuditLogDatatableDTO> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		// column 2 is auditLogOperation - lot of null checks and a check if we are searching on logAction
		if (input != null && input.getColumns() != null && input.getColumns().get(2) != null && input.getColumns().get(2).getSearch() != null && input.getColumns().get(2).getSearch().getValue() != null && !input.getColumns().get(2).getSearch().getValue().equals("")) {
			String searchTerm = input.getColumns().get(2).getSearch().getValue();
			Specification<AuditLogDatatable> auditLogByLogAction = getAuditLogByOperation(searchTerm);
			return convertAuditLogDataTablesModelToDTO(auditLogDatatableDao.findAll(input, null, auditLogByLogAction));
		}

		return convertAuditLogDataTablesModelToDTO(auditLogDatatableDao.findAll(input));
	}

	private DataTablesOutput<AuditLogDatatableDTO> convertAuditLogDataTablesModelToDTO(DataTablesOutput<AuditLogDatatable> output) {
		List<AuditLogDatatableDTO> dataWithMessages = output.getData().stream().map(AuditLogDatatableDTO::new).collect(Collectors.toList());

		DataTablesOutput<AuditLogDatatableDTO> result = new DataTablesOutput<>();
		result.setData(dataWithMessages);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}

	private Specification<AuditLogDatatable> getAuditLogByOperation(String search) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("operation"), AuditLogOperation.valueOf(search));
	}
}
