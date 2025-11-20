package dk.digitalidentity.medcommailbox.security.filter;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
// https://medium.com/@briancheruiyot00/securing-swagger-ui-with-basic-authentication-login-popup-in-spring-boot-8b8bdd9ca931
public class SwaggerBasicAuthFilter extends OncePerRequestFilter {
    @Autowired
    private MedcomMailboxConfiguration configuration;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        final String requestUri = request.getRequestURI();

        if (isRestrictedUrl(requestUri)) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String[] credentials = extractAndDecodeHeader(authHeader);

                if (isAuthorized(credentials[0], credentials[1])) {
                    chain.doFilter(request, response);
                    return;
                }
            }
            response.setHeader("WWW-Authenticate", "Basic realm=\"Swagger\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }


    private boolean isRestrictedUrl(final String requestUri) {
        return requestUri.startsWith("/v3/api-docs") || requestUri.startsWith("/swagger-ui");
    }

    private boolean isAuthorized(final String username, final String password) {
        return username.equals(configuration.getSwagger().getUsername()) && password.equals(configuration.getSwagger().getPassword());
    }

    private String[] extractAndDecodeHeader(final String header) throws IOException {
        byte[] base64Token = header.substring(6).getBytes(StandardCharsets.UTF_8);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to decode basic authentication token");
        }

        String token = new String(decoded, StandardCharsets.UTF_8);
        int delim = token.indexOf(":");
        if (delim == -1) {
            throw new RuntimeException("Invalid basic authentication token");
        }

        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }
}
