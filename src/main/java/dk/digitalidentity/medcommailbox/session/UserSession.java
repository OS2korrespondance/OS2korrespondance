package dk.digitalidentity.medcommailbox.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;

@Component
@SessionScope
@Getter
@Setter
public class UserSession implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

    private LandingInfo landingInfo;

    private LandingInfo getAndResetLandingInfo() {
        final LandingInfo result = landingInfo;
        landingInfo = null;
        return result;
    }

}
