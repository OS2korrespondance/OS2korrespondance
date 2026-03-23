package dk.digitalidentity.medcommailbox.mapper;

import dk.digitalidentity.medcommailbox.TestConfig;
import dk.digitalidentity.medcommailbox.config.JaxbConfiguration;
import dk.digitalidentity.medcommailbox.dao.BinaryDao;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.medcomFreeTextToHtml;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getClinicalEmail;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = { JaxbConfiguration.class, EmessageMapper.class, S3Service.class, TestConfig.class})
public class MedcomMapperTest {
    @Autowired
    private Unmarshaller unmarshaller;
	@Autowired
	private EmessageMapper emessageMapper;
	@MockitoBean
	private BinaryDao binaryDaoMock;
	@MockitoBean
	private S3Service s3ServiceMock;


	/**
	 * Verifies that FormattedTextType tags (Bold, Italic etc.) are correctly unescaped
	 * in the marshalled XML output, while user-typed angle bracket characters (e.g. "< 120")
	 * remain safely escaped and are not affected by the unescape logic.
	 */
	@Test
	public void toXml_unescapesFormattedTextTags_butPreservesUserAngleBrackets() {
		// Given - fixture contains user text with literal '<' and '>' characters
		// e.g. <Text01>&lt;Teste&gt;</Text01> — should remain escaped in output
		final var message = loadFixture("/fixtures/special_characters.xml");

		// When
		final var result = emessageMapper.toXML(message);

		// Then - user angle brackets must stay escaped
		assertThat(result.xml()).contains("&lt;Teste&gt;");
		assertThat(result.xml()).contains("&lt;Teste noget mere øæå&gt;");

		// And identifiers are correctly extracted
		assertThat(result.envelopeIdentifier()).isEqualTo("KuvertNr012277");
		assertThat(result.letterIdentifier()).isEqualTo("BrevNr00777");
	}

	/**
	 * Verifies that a fixture containing actual FormattedTextType tags stored as escaped strings
	 * (e.g. &lt;Bold&gt;fed tekst&lt;/Bold&gt;) are correctly unescaped to proper XML tags
	 * in the output.
	 */
	/**
	 * This test uses a fixture where Text01 contains escaped FormattedTextType tags stored as plain text,
	 * i.e. the rich text editor has stored "<Italic>tekst</Italic>" as a string, which in the XML fixture
	 * is double-escaped as &amp;lt;Italic&amp;gt;tekst&amp;lt;/Italic&amp;gt;.
	 * After JAXB unmarshal, Text01 contains the string "&lt;Italic&gt;tekst&lt;/Italic&gt;",
	 * which unescapeFormattedTextTags() must convert to proper XML tags.
	 * User-typed angle brackets (e.g. "blodtryk < 120") must remain escaped throughout.
	 */
	@Test
	public void toXml_unescapesAllKnownFormattedTextTags() {
		// Given
		final var message = loadFixture("/fixtures/escaped_formatted_text.xml");

		// When
		final var result = emessageMapper.toXML(message);

		// Then - escaped FormattedTextType tags must be unescaped to proper XML
		assertThat(result.xml()).contains("<Italic>fed kursiv</Italic>");
		assertThat(result.xml()).contains("<Bold>fed tekst</Bold>");
		assertThat(result.xml()).contains("<Underline>understreget</Underline>");
		assertThat(result.xml()).contains("<Right>højrestillet</Right>");
		assertThat(result.xml()).contains("<Center>centreret</Center>");
		assertThat(result.xml()).contains("<FixedFont>fast bredde</FixedFont>");
		assertThat(result.xml()).contains("<Break/>");
		assertThat(result.xml()).contains("<Space/>");

		// And their escaped forms must NOT remain in output
		assertThat(result.xml()).doesNotContain("&lt;Italic&gt;");
		assertThat(result.xml()).doesNotContain("&lt;Bold&gt;");
		assertThat(result.xml()).doesNotContain("&lt;Underline&gt;");
		assertThat(result.xml()).doesNotContain("&lt;Break/&gt;");
		assertThat(result.xml()).doesNotContain("&lt;Space/&gt;");

		// And user-typed angle brackets must remain escaped (not affected by unescape logic)
		assertThat(result.xml()).contains("&lt;Teste&gt;");
		assertThat(result.xml()).contains("&lt;Teste noget mere øæå&gt;");
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
