package dk.digitalidentity.medcommailbox.controller;

import dk.digitalidentity.medcommailbox.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This class checks that no controller allows unauthenticated requests.
 * It does not test all endpoints but, so it is expected authentication annotations are placed on the controller classes.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
public class MvcSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void failsWhenUnauthenticated() throws Exception {
        assertUrlProtected("/admin");
        assertUrlProtected("/auditlog");
        assertUrlProtected("/landing");
        assertUrlProtected("/mailbox");
        assertUrlProtected("/logs");
    }

    private void assertUrlProtected(final String url) throws Exception {
        mockMvc.perform(get(url))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/saml2/authenticate/IdP"));
    }

}
