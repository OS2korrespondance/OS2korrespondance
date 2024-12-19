package dk.digitalidentity.medcommailbox.security;

import dk.digitalidentity.medcommailbox.config.RoleConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class SecurityUtil {
	public static boolean isLoggedIn() {
		return SecurityContextHolder.getContext().getAuthentication() != null;
	}

	public static String getUserIP() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return "0.0.0.0";
		}

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

		return request.getRemoteAddr();
	}

	public String getUserId() {
		if (!isLoggedIn()) {
			return null;
		}
		Object principal = SecurityContextHolder.getContext().getAuthentication().getName();
		return (principal instanceof String) ? (String) principal : null;
	}

	public static Set<String> getLocationNumberConstraint(final String locationNumberName) {
		if (isLoggedIn()) {
			Set<String> numbers = new HashSet<>();
			for (SamlGrantedAuthority authority : (List<SamlGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				if (authority.getConstraints() != null) {
					for (SamlGrantedAuthority.Constraint constraint : authority.getConstraints()) {
						if (Objects.equals(authority.getAuthority(), RoleConstants.USER) &&
								Objects.equals(constraint.getConstraintType(), locationNumberName)) {
							numbers.addAll(Arrays.asList(constraint.getConstraintValue().split(",")));
						}
					}
				}
			}
			return numbers;
		}

		return null;
	}
}
