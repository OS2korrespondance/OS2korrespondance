package dk.digitalidentity.medcommailbox.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.datatables.repository.DataTablesRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class, basePackages = "dk.digitalidentity.medcommailbox.datatables")
@EntityScan(basePackages = "dk.digitalidentity.medcommailbox.datatables")
public class DataTablesConfiguration {

}
