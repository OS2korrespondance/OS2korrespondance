package dk.digitalidentity.medcommailbox.security;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.RoleConstants;
import dk.digitalidentity.medcommailbox.config.Sender;
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
import java.util.stream.Collectors;

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

	public static Set<String> getLocationNumberConstraint(final String locationNumberName, final MedcomMailboxConfiguration configuration) {
		if (!isLoggedIn()) {
			throw new IllegalStateException("Not logged in");
		}
		//noinspection unchecked
		final List<SamlGrantedAuthority> authorities =
				(List<SamlGrantedAuthority>) SecurityContextHolder
						.getContext()
						.getAuthentication()
						.getAuthorities();
		boolean isConstrained = authorities.stream()
				.filter(authority -> authority.getConstraints() != null)
				.flatMap(authority -> authority.getConstraints().stream())
				.anyMatch(constraint -> Objects.equals(constraint.getConstraintType(), locationNumberName));

		if (isConstrained) {
			return authorities.stream()
					.filter(authority -> authority.getConstraints() != null)
					.flatMap(authority -> authority.getConstraints().stream())
					.filter(constraint -> Objects.equals(constraint.getConstraintType(), locationNumberName))
					.map(SamlGrantedAuthority.Constraint::getConstraintValue)
					.flatMap(constraintValue -> Arrays.stream(constraintValue.split(",")))
					.collect(Collectors.toSet());
		} else {
			return configuration.getSenders().stream()
					.map(Sender::getEanIdentifier)
					.collect(Collectors.toSet());
		}
	}
}
