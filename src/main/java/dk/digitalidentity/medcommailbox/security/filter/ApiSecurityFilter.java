package dk.digitalidentity.medcommailbox.security.filter;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class ApiSecurityFilter implements Filter {

	@Setter
	private MedcomMailboxConfiguration medcomMailboxConfiguration;

    @Setter
    private boolean enabled;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

		// Tillad OPTIONS requests (CORS preflight) uden ApiKey check
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			// Tilføj CORS headers til OPTIONS response
			response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
			response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			response.setHeader("Access-Control-Allow-Headers", "ApiKey, Content-Type, Accept");
			response.setHeader("Access-Control-Allow-Credentials", "true");
			response.setHeader("Access-Control-Max-Age", "3600");
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

        String authHeader = request.getHeader("ApiKey");
        if (!enabled) {
            unauthorized(response, "API (MedCom Mailbox) is not enabled", authHeader);
            return;
        }

        if (StringUtils.hasLength(authHeader)) {
            boolean apiKeyMatch = checkApiKey(authHeader);
			if (!apiKeyMatch) {
                unauthorized(response, "Invalid ApiKey for (MedCom Mailbox)", authHeader);
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
        }
        else {
            unauthorized(response, "Missing ApiKey for (MedCom Mailbox)", null);
        }
    }

    private void unauthorized(HttpServletResponse response, String message, String authHeader) throws IOException {
        log.warn(message + " (authHeader = " + authHeader + ")");

        response.sendError(401, message);
    }

    private boolean checkApiKey(String apiKey) {
		String entry = medcomMailboxConfiguration.getSwagger().getApiKey();
        return entry != null && Objects.equals(entry, apiKey);
    }
}
