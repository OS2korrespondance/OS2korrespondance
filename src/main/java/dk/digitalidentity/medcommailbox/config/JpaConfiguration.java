package dk.digitalidentity.medcommailbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EnableJpaRepositories(basePackages = { "dk.digitalidentity.medcommailbox.dao" }, repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
public class JpaConfiguration {

}