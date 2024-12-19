package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.ResourceResolver;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.config.JaxbConfiguration;
import dk.digitalidentity.medcommailbox.dao.BinaryDao;
import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.Patient;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.dao.model.enums.EpisodeOfCareStatusCode;
import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import static org.xmlunit.assertj.XmlAssert.assertThat;


@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = { MailService.class, EmessageMapper.class, TemplateEngine.class, JaxbConfiguration.class})
@EnableConfigurationProperties(value = MedcomMailboxConfiguration.class)
public class MailServiceTest {
    @Autowired
    private MailService mailService;
    @Autowired
    private EmessageMapper emessageMapper;
    @MockBean
    private MedcomLogService logServiceMock;
    @MockBean
    private MailDao mailDaoMock;
    @MockBean
    private S3Service s3ServiceMock;
    @MockBean
    private RecipientService recipientServiceMock;
    @MockBean
    private FailedS3KeyService failedS3KeyService;
    @MockBean
    private BinaryDao binaryDao;

    /**
     * This test will simply check that the Mail to Xml conversion works and is schema valid
     */
    @Test
    public void mailMessageValidates() throws SAXException {
        // Given
        final var mail = mockMail();
        final var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new ResourceResolver("xsd/2006/07/01/"));
        final var xsdURL = this.getClass().getResource("/xsd/2006/07/01/EMessage.xsd");
        final var schema = schemaFactory.newSchema(xsdURL);

        // When
        final var msg = emessageMapper.fromMailToEmessage(mail, createSender());
        final var result = emessageMapper.toXML(msg);

        // Then
        assertThat(result.xml()).isValidAgainst(schema);
    }

    private static Mail mockMail() {
        final var mail = new Mail();
        mail.setSubject("Et emne");
        mail.setContent("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nec pharetra sapien. Sed imperdiet, odio id dictum consectetur, elit massa viverra tortor, ut mattis est ex non velit. Sed faucibus et velit efficitur lobortis. Praesent nec viverra ante. Nulla consectetur nisi sollicitudin arcu ultrices commodo. Nam viverra sapien sit amet lacus aliquam, eget tristique elit interdum. Sed a sagittis nisi. Cras vel libero imperdiet, blandit nisi et, ullamcorper lectus. Morbi consequat elementum felis, eget tempor ante pretium id. Duis convallis, felis ut lacinia bibendum, erat sapien bibendum erat, eget interdum elit mauris nec diam.");
        mail.setRecipient(createRecipient());
        mail.setPatient(createPatient());
        return mail;
    }

    private static Patient createPatient() {
        final var patient = new Patient();
        patient.setCpr("1111111118");
        patient.setName("Et patient navn");
        patient.setAlternativeIdentifier(false);
        patient.setEpisodeOfCareStatusCode(EpisodeOfCareStatusCode.inaktiv);
        patient.setPostCodeIdentifier("8000");
        patient.setDistrictName("Århus C");
        patient.setStreetName("Gunnar Clausens vej");
        return patient;
    }

    private static Sender createSender() {
        final var sender = new Sender();
        sender.setEanIdentifier("123456789");
        sender.setIdentifierCode(IdentifierCode.lokationsnummer);
        sender.setIdentifier("123456789");
        sender.setOrganisationName("En afsender");
        return sender;
    }

    private static Recipient createRecipient() {
        final var recipient = new Recipient();
        recipient.setEanIdentifier("222222222");
        recipient.setIdentifierCode(IdentifierCode.lokationsnummer);
        recipient.setIdentifier("22222222");
        recipient.setShortOrganisationName("En modtager");
        recipient.setFullOrganisationName("En modtager");
        return recipient;
    }

}
