package dk.digitalidentity.medcommailbox.mapper;
//JaxbConfiguration

import dk.digitalidentity.medcommailbox.config.JaxbConfiguration;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.medcomFreeTextToHtml;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getClinicalEmail;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = { JaxbConfiguration.class})
public class MedcomMapperTest {
    @Autowired
    private Unmarshaller unmarshaller;

    @Test
    public void canMapFreeText() {
        // Given
        final var message = loadFixture("/fixtures/formattet_text.xml");
        final var clinicalEmail = getClinicalEmail(message).orElseGet(Assertions::fail);

        // When
        final var clinicalInformationHtml = clinicalEmail.getClinicalInformations().stream()
                .map(c -> medcomFreeTextToHtml(c.getText01()))
                .collect(Collectors.joining());

        // Then
        assertThat(clinicalInformationHtml).isEqualTo("<pre>Hej, jeg er fast bredde</pre><u>Afdeling&nbsp;A</u><br/><em>har modtaget en henvisning på ovenstående patient.</em><br/><p style=\"text-align: right;\">Forinden der foretages visitation, bedes følgende undersøgelser foretaget:</p><br/>Røntgen af hofte i to planer.<br/>Blodtryksresultat<br/>Sænkningsresultat<br/><br/>Resultaterne bedes fremsendt som et \"korrespondancebrev\".<p style=\"text-align: center;\"><br/><br/>Venlig hilsen<br/>overlæge K. Petersen<br/>15.01.1999<br/></p>");
    }

    @Test
    public void canMapSpecialCharacters() {
        // Given
        final var message = loadFixture("/fixtures/special_characters.xml");
        final var clinicalEmail = getClinicalEmail(message).orElseGet(Assertions::fail);

        // When
        final var clinicalInformationHtml = clinicalEmail.getClinicalInformations().stream()
                .map(c -> medcomFreeTextToHtml(c.getText01()))
                .collect(Collectors.joining());

        // Then
        assertThat(clinicalInformationHtml).isEqualTo("<Teste><Teste noget mere øæå>");
    }

    private Emessage loadFixture(final String path) {
        try (InputStream resourceAsStream = MedcomMapperTest.class.getResourceAsStream(path)) {
            return (Emessage) unmarshaller.unmarshal(resourceAsStream);
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
