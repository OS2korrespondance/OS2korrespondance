package dk.digitalidentity.medcommailbox.config;

import dk.digitalidentity.medcommailbox.security.filter.ApiSecurityFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApiSecurityFilterConfiguration {

	private final MedcomMailboxConfiguration configuration;

	@Bean
	public FilterRegistrationBean<ApiSecurityFilter> auditLogApiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter();
		filter.setEnabled(configuration.getSwagger().isEnabled());
		filter.setMedcomMailboxConfiguration(configuration);

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.setName("ApiSecurityBean");
		filterRegistrationBean.addUrlPatterns("/api/*");
		filterRegistrationBean.setOrder(1);

		return filterRegistrationBean;
	}

}
