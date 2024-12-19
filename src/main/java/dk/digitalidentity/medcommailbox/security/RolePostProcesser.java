package dk.digitalidentity.medcommailbox.security;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.RoleConstants;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class RolePostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private MedcomMailboxConfiguration config;

	@Override
	public void process(TokenUser tokenUser) {
		List<SamlGrantedAuthority> newAuthorities = new ArrayList<>();
		String adminRole = "ROLE_" + config.getAdminRoleId();
		String userRole = "ROLE_" + config.getUserRoleId();

		for (Iterator<? extends SamlGrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			SamlGrantedAuthority grantedAuthority = iterator.next();
			if (adminRole.equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.ADMIN));
			} else if (userRole.equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER, grantedAuthority.getConstraints()));
			}
		}

		// no roles, no access
		if (newAuthorities.size() == 0) {
			throw new InsufficientAuthenticationException("Ingen roller til systemet - adgang nægtet");
		}

		tokenUser.setAuthorities(newAuthorities);

		auditLogService.log(AuditLogOperation.USER_LOGGED_IN, null, tokenUser.getUsername());
	}
}
