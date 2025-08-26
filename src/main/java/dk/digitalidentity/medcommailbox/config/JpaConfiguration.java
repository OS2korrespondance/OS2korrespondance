package dk.digitalidentity.medcommailbox.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EnableJpaRepositories(basePackages = { "dk.digitalidentity.medcommailbox.dao" }, repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
@EntityScan(basePackages = "dk.digitalidentity.medcommailbox.dao.model")
public class JpaConfiguration {

}