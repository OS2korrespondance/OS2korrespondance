package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.controller.rest.korrespondance.CaseIdWrapper;
import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetter;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedcomSenderService {
    private final EmessageMapper emessageMapper;
    private final S3Service s3Service;
    private final MedcomLogService logService;
    private final MedcomMailboxConfiguration configuration;
    private final MailService mailService;

    @Transactional
    public void sendMessage(final Mail draft, final Sender sender) {
        final Emessage emessage = emessageMapper.fromMailToEmessage(draft, sender);
        EmessageMapper.ToXmlResult result = emessageMapper.toXML(emessage);
        if (result == null || StringUtils.isEmpty(result.xml())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke omdanne beskeden til xml, den blev derfor ikke sendt");
        }
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid+"_"+draft.getSent().format(dateFormatter);
        String key = s3Service.upload(FolderConstants.FOLDER_OUT, fileName + ".xml", getXmlBytes(result.xml()));
        draft.setEnvelopeIdentifier(result.envelopeIdentifier());
        draft.setLetterIdentifier(result.letterIdentifier());
        draft.setS3FileKey(key);
        archiveMessage(draft, emessage, fileName);
        log(result.xml(), result.envelopeIdentifier(), result.letterIdentifier());
    }

    @Transactional
    public void sendBinaryMessage(final Mail draft, Recipient recipient, final Sender sender) {
        final BinaryMessage binaryMessage = draft.getBinaryMessage();
        binaryMessage.setPatient(draft.getPatient());
        binaryMessage.setIncomming(false);
        binaryMessage.setRecipient(recipient);
        dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage binary = emessageMapper.fromBinaryToMessage(binaryMessage, sender);
        final EmessageMapper.ToXmlResult binResult = emessageMapper.binaryToXML(binary);
        if (binResult == null || StringUtils.isEmpty(binResult.xml())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke omdanne beskeden til xml, den blev derfor ikke sendt");
        }
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        final String uuid = UUID.randomUUID().toString();
        final String fileName = uuid+"_"+draft.getSent().format(dateFormatter);
        // Do we need the binkey saved somewhere?
        final String binKey =
                s3Service.upload(FolderConstants.FOLDER_OUT, fileName + ".xml", getXmlBytes(binResult.xml()));
        binaryMessage.setEnvelopeIdentifier(binResult.envelopeIdentifier());
        binaryMessage.setLetterIdentifier(binResult.letterIdentifier());

        // Remove the data so we can save to the medcom log
        binary.getHistopathologyReportsAndBinaryLettersAndLocalElements().stream()
                        .map(m -> (BinaryLetter)m)
                        .flatMap(m -> m.getBinaryObjects().stream())
                        .forEach(b -> b.setObjectBase64Encoded(""));
        final EmessageMapper.ToXmlResult xmlWithoutData = emessageMapper.binaryToXML(binary);
        log(xmlWithoutData.xml(), binResult.envelopeIdentifier(), binResult.letterIdentifier());
    }

    private void log(final String xml, final String envelopeIdentifier, final String letterIdentifier) {
        MedcomLog log = new MedcomLog();
        log.setMailTts(LocalDateTime.now());
        log.setMailXml(xml);
        log.setEnvelopeIdentifier(envelopeIdentifier);
        log.setLetterIdentifier(letterIdentifier);
        log.setIncomming(false);
        logService.save(log);
    }

    private void archiveMessage(final Mail draft, final Emessage emessage, final String fileName) {
        if (!configuration.isCreateArchives()) {
            log.info("Not creating archive, feature disabled");
        }
        // Add elements that should only be saved in the archive file
        if (isNotEmpty(draft.getCaseId())) {
            emessage.getClinicalEmailsAndLaboratoryAnalysisFilesAndLaboratoryRequests().add(new CaseIdWrapper(draft.getCaseId()));
        }
        final EmessageMapper.ToXmlResult archiveXmlResult = emessageMapper.toXML(emessage);
        mailService.toArchive(draft, fileName, getXmlBytes(archiveXmlResult.xml()));
    }

    private static byte[] getXmlBytes(final String xml) {
        return xml.getBytes(StandardCharsets.ISO_8859_1);
    }
}
