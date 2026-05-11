package dk.digitalidentity.medcommailbox.dao;

import dk.digitalidentity.medcommailbox.model.entity.MonitoringState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringStateDao extends JpaRepository<MonitoringState, Long> {
}
