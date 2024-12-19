package dk.digitalidentity.medcommailbox.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
@Getter
@Setter
public class UserSession {

    private LandingInfo landingInfo;

    private LandingInfo getAndResetLandingInfo() {
        final LandingInfo result = landingInfo;
        landingInfo = null;
        return result;
    }

}
