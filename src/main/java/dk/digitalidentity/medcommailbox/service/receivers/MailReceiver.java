package dk.digitalidentity.medcommailbox.service.receivers;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.model.FailedS3Key;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.dao.model.Patient;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.dao.model.Reference;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.dao.model.enums.ReceiptType;
import dk.digitalidentity.medcommailbox.dao.model.enums.Status;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import dk.digitalidentity.medcommailbox.service.BinaryService;
import dk.digitalidentity.medcommailbox.service.FailedS3KeyService;
import dk.digitalidentity.medcommailbox.service.MailService;
import dk.digitalidentity.medcommailbox.service.MedcomLogService;
import dk.digitalidentity.medcommailbox.service.ReceiptHandler;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.service.receivers.exceptions.UnknownReceiverException;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.AcknowledgementCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalEmail;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalInformationNotSignedType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.IdentifierCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.VersionCodeType;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.fromMedcom;
import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.medcomFreeTextToHtml;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getClinicalEmail;

@Slf4j
@Component
public class MailReceiver implements MedcomReceiver {
    @Autowired
    private MailService mailService;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private FailedS3KeyService failedS3KeyService;
    @Autowired
    private RecipientService recipientService;
    @Autowired
    private EmessageMapper emessageMapper;
    @Autowired
    private MedcomLogService logService;
    @Autowired
    private Marshaller marshaller2005;
    @Autowired
    private MedcomMailboxConfiguration config;
    @Autowired
    private BinaryService binaryService;

    @Override
    public boolean isHandled(final String s3Key) {
        final List<String> existingMailKeys = mailService.findByOriginalFolderAndDraftFalseAndS3FileKeyNotNull(Folder.INBOX).stream().map(Mail::getS3FileKey).toList();
        return existingMailKeys.contains(s3Key);
    }

    @Override
    public boolean supports(final MedcomXml medcomXml) {
        return medcomXml.getParsedValue() instanceof Emessage;
    }

    @Transactional
    @Override
    public void handle(final MedcomXml medcomXml) {
        if (!(medcomXml.getParsedValue() instanceof Emessage message))  {
            throw new IllegalArgumentException("Emessage object must be of type Emessage");
        }
        final ClinicalEmail clinicalEmail = getClinicalEmail(message).orElseThrow();
        final String s3Key = medcomXml.getS3Key();

        // we have a log on the identifiers already (= it has been received before)
        MedcomLog existingLog = logService.getByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(message.getEnvelope().getIdentifier(), clinicalEmail.getLetter().getIdentifier()).orElse(null);
        if (existingLog != null) {
            log.warn("Mail with envelope identifier {} and letter identifier {} has been loaded before. The s3 fileKey of this mail is {}. Will send new receipt if asked for.", message.getEnvelope().getIdentifier(), clinicalEmail.getLetter().getIdentifier(), s3Key);

            if (existingLog.getReceiptType() != null && existingLog.getReceiptType().equals(ReceiptType.POSITIVE)) {
                if (message.getEnvelope().getAcknowledgementCode().equals(AcknowledgementCodeType.PLUSPOSITIVKVITT)) {
                    log.info("Sending new positive receipt for with envelope identifier {} and letter identifier {}", message.getEnvelope().getIdentifier(), clinicalEmail.getLetter().getIdentifier());
                    sendPositiveReceipt(message, existingLog, true);
                }
            }
            else if (existingLog.getReceiptType() != null && existingLog.getReceiptType().equals(ReceiptType.NEGATIVE)) {
                if (!clinicalEmail.getLetter().getVersionCode().equals(VersionCodeType.XD_9134_L)) {
                    log.info("Sending new negative (wrong verison code) receipt for with envelope identifier {} and letter identifier {}", message.getEnvelope().getIdentifier(), clinicalEmail.getLetter().getIdentifier());
                    sendWrongVersionNegativeReceipt(message, medcomXml.getFileContents(), existingLog);
                }
            }

            FailedS3Key failed = new FailedS3Key();
            failed.setS3FileKey(s3Key);
            failedS3KeyService.save(failed);
            return;
        }
        if (!clinicalEmail.getLetter().getVersionCode().equals(VersionCodeType.XD_9134_L)) {
            log.error("Mail with s3 key {} has the wrong versionCode. Will send negative receipt.", s3Key);

            FailedS3Key failed = new FailedS3Key();
            failed.setS3FileKey(s3Key);
            failedS3KeyService.save(failed);

            sendWrongVersionNegativeReceipt(message, medcomXml.getFileContents(), null);
            return;
        }

        if (config.getSenders().stream().noneMatch(s -> s.getEanIdentifier().equals(clinicalEmail.getReceiver().getEANIdentifier()))) {
            log.error("Mail with s3 key {} has a recipient identifier that are not configured on this instance: {}. Skipping this time. You have to update the configuration to receive this mail.", s3Key, clinicalEmail.getReceiver().getIdentifier());
            return;
        }

        final Mail mail = new Mail();

        String str = message.getEnvelope().getSent().getDate() + " " + message.getEnvelope().getSent().getTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        mail.setReceived(dateTime);

        mail.setDraft(false);

        Recipient sender = recipientService.findByEanIdentifier(clinicalEmail.getSender().getEANIdentifier()).orElse(null);
        if (sender == null) {
            sender = new Recipient();
            sender.setEanIdentifier(clinicalEmail.getSender().getEANIdentifier());
            sender.setIdentifier(clinicalEmail.getSender().getIdentifier());
            sender.setIdentifierCode(fromMedcom(clinicalEmail.getSender().getIdentifierCode()));
            sender.setShortOrganisationName(clinicalEmail.getSender().getOrganisationName());
            sender.setFullOrganisationName(clinicalEmail.getSender().getOrganisationName());
            sender = recipientService.save(sender);
        }
        sender.setDepartmentName(clinicalEmail.getSender().getDepartmentName());
        sender.setUnitName(clinicalEmail.getSender().getUnitName());
        sender.setStreetName(clinicalEmail.getSender().getStreetName());
        sender.setDistrictName(clinicalEmail.getSender().getDistrictName());
        sender.setPostcodeIdentifier(clinicalEmail.getSender().getPostCodeIdentifier());
        sender.setPhoneNumber(clinicalEmail.getSender().getTelephoneSubscriberIdentifier());
        mail.setSender(sender);
        mail.setSubject(clinicalEmail.getAdditionalInformation() != null && clinicalEmail.getAdditionalInformation().getSubject() != null ? clinicalEmail.getAdditionalInformation().getSubject() : "Intet emne");

        if (clinicalEmail.getSender().getDepartmentName() != null) {
            mail.setSenderDepartmentName(clinicalEmail.getSender().getDepartmentName());
        }

        if (clinicalEmail.getSender().getMedicalSpecialityCode() != null) {
            mail.setSenderMedicalSpecialityCode(fromMedcom(clinicalEmail.getSender().getMedicalSpecialityCode()));
        }

        StringBuilder content = new StringBuilder();
        for (ClinicalInformationNotSignedType clinicalInfo : clinicalEmail.getClinicalInformations()) {
            content.append(medcomFreeTextToHtml(clinicalInfo.getText01()));
        }
        mail.setContent(content.toString());

        mail.setPatient(createPatient(clinicalEmail));
        mail.setS3FileKey(s3Key);
        mail.setFolder(Folder.INBOX);
        mail.setOriginalFolder(Folder.INBOX);
        mail.setStatus(Status.DONE);
        mail.setEnvelopeIdentifier(message.getEnvelope().getIdentifier());
        mail.setLetterIdentifier(clinicalEmail.getLetter().getIdentifier());
        mail.setHighPriority(clinicalEmail.getAdditionalInformation().getPriority().equals("høj_prioritet"));
        if (IdentifierCodeType.LOKATIONSNUMMER == clinicalEmail.getReceiver().getIdentifierCode()) {
            mail.setAssociatedIdentifier(clinicalEmail.getReceiver().getIdentifier());
        } else {
            mail.setAssociatedIdentifier(clinicalEmail.getReceiver().getEANIdentifier());
        }
        clinicalEmail.getReferences().forEach(ref -> {
                    final Reference reference = fromMedcom(ref);
                    reference.setMail(mail);
                    mail.getReferences().add(reference);
                    // Set binaryMessage in case it was already recevied
                    binaryService.findByIdentifier(reference.getObjectIdentifier())
                            .ifPresent(b -> mail.setBinaryMessage(b.getMessage()));
                });

        final Mail savedMail = mailService.save(mail);

        String[] fileNameSplit = s3Key.split("/");
        String fileName = fileNameSplit[fileNameSplit.length - 1].replace(".xml", "").replace(".encrypted", "");
        mailService.toArchive(savedMail, fileName, medcomXml.getFileContents());

        MedcomLog log = new MedcomLog();
        log.setMailTts(LocalDateTime.now());
        log.setMailXml(new String(medcomXml.getFileContents(), StandardCharsets.ISO_8859_1));
        log.setEnvelopeIdentifier(message.getEnvelope().getIdentifier());
        log.setLetterIdentifier(clinicalEmail.getLetter().getIdentifier());
        log.setIncomming(true);

        // positiv kvittering hvis pluspositivkvitt
        if (message.getEnvelope().getAcknowledgementCode().equals(AcknowledgementCodeType.PLUSPOSITIVKVITT)) {
            sendPositiveReceipt(message, log, false);
        }

        logService.save(log);
    }

    private static Patient createPatient(final ClinicalEmail clinicalEmail) {
        Patient patient = new Patient();
        patient.setCpr(clinicalEmail.getPatient().getCivilRegistrationNumber() == null ? clinicalEmail.getPatient().getAlternativeIdentifier() : clinicalEmail.getPatient().getCivilRegistrationNumber());
        if (clinicalEmail.getPatient().getCivilRegistrationNumber() == null) {
            patient.setAlternativeIdentifier(true);
        }
        String firstName = clinicalEmail.getPatient().getPersonGivenName() == null ? "" : clinicalEmail.getPatient().getPersonGivenName() + " ";
        patient.setName(firstName + clinicalEmail.getPatient().getPersonSurnameName());
        patient.setEpisodeOfCareStatusCode(fromMedcom(clinicalEmail.getPatient().getEpisodeOfCareStatusCode()));
        patient.setStreetName(clinicalEmail.getPatient().getStreetName());
        patient.setSuburbName(clinicalEmail.getPatient().getSuburbName());
        patient.setDistrictName(clinicalEmail.getPatient().getDistrictName());
        patient.setPostCodeIdentifier(clinicalEmail.getPatient().getPostCodeIdentifier());
        return patient;
    }

    @Override
    @Transactional
    public boolean handleReceipt(final ReceiptHandler.ReceiptResult receiptResult) {
        try {
            final Mail mail = mailService.findIncomingByEnvelopeIdentifierAndLetterIdentifier(
                    receiptResult.getEnvelopeIdentifier(), receiptResult.getLetterIdentifier(), false);
            if (mail != null) {
                mail.setReceivedNegativeReceipt(receiptResult.getReceiptType() != ReceiptType.POSITIVE);
                mailService.save(mail);
                return true;
            }
        } catch (final Exception e) {
            log.error("Failed to handle receipt {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void negativeReceipt(final MedcomXml xml, final Exception ex) {
        if (!(xml.getParsedValue() instanceof Emessage emessage))  {
            log.error("Do not know what to do with {}", xml.getS3Key());
            return;
        }
        final ClinicalEmail clinicalEmail = getClinicalEmail(emessage).orElseThrow();
        final LocalDateTime now = LocalDateTime.now();
        final String refuseText = refuseTextFromException(ex);
        final String refuseCode = refuseCodeFromException(ex);
        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage negReceipt = emessageMapper.createNegativeReceipt(emessage, now, refuseText, refuseCode);
        final String negativeXml = marshal(marshaller2005, negReceipt);
        final byte[] file = negativeXml.getBytes(StandardCharsets.ISO_8859_1);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        final String uuid = UUID.randomUUID().toString();
        final String fileKey = uuid+"_negative_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);

        MedcomLog log = new MedcomLog();
        log.setMailXml(new String(xml.getFileContents(), StandardCharsets.ISO_8859_1));
        log.setMailTts(now);
        log.setIncomming(true);
        log.setReceiptType(ReceiptType.NEGATIVE);
        log.setEnvelopeIdentifier(emessage.getEnvelope().getIdentifier());
        log.setLetterIdentifier(clinicalEmail.getLetter().getIdentifier());
        log.setNegativeReceiptReason(ex.getLocalizedMessage());
        log.setReceiptXml(negativeXml);
        log.setReceiptTts(now);
        log.setReceiptS3FileKey(fileKey);
        logService.save(log);

        FailedS3Key failedS3Key = new FailedS3Key();
        failedS3Key.setS3FileKey(xml.getS3Key());
        failedS3KeyService.save(failedS3Key);
    }

    private void sendPositiveReceipt(Emessage message, MedcomLog mailReceiptLog, boolean save) {
        final LocalDateTime now = LocalDateTime.now();
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = emessageMapper.createPositiveReceipt(message, now);
        final String xml = marshal(marshaller2005, eMessage);

        byte[] file = xml.getBytes(StandardCharsets.ISO_8859_1);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String uuid = UUID.randomUUID().toString();
        String fileKey = uuid+"_positive_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);

        mailReceiptLog.setReceiptTts(now);
        mailReceiptLog.setReceiptType(ReceiptType.POSITIVE);
        mailReceiptLog.setReceiptXml(xml);
        mailReceiptLog.setReceiptS3FileKey(fileKey);

        if (save) {
            logService.save(mailReceiptLog);
        }
    }

    private void sendWrongVersionNegativeReceipt(Emessage message, byte[] xml, MedcomLog existingLog) {
        LocalDateTime now = LocalDateTime.now();
        final ClinicalEmail clinicalEmail = getClinicalEmail(message).orElseThrow();
        String refuseText = "Brev med nummeret " + clinicalEmail.getLetter().getIdentifier() + ", afsendt " + message.getEnvelope().getSent().getDate() + " kl. " + message.getEnvelope().getSent().getTime() + " kunne ikke modtages på grund af forkert versionsnummer. Vi modtager kun beskeder med versionsnummer XD9134L og af typen korrespondance.<Break/>Med venlig hilsen<Break/>Fælles forløb";
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = emessageMapper.createNegativeReceipt(message, now, refuseText, "problem_med_version");

        String negativeXml = marshal(marshaller2005, eMessage);
        if (StringUtils.isEmpty(negativeXml)) {
            return;
        }

        byte[] file = negativeXml.getBytes(StandardCharsets.ISO_8859_1);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        String uuid = UUID.randomUUID().toString();
        String fileKey = uuid+"_negative_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);

        if (existingLog == null) {
            existingLog = new MedcomLog();
            existingLog.setMailXml(new String(xml, StandardCharsets.ISO_8859_1));
            existingLog.setMailTts(now);
            existingLog.setIncomming(true);
            existingLog.setReceiptType(ReceiptType.NEGATIVE);
            existingLog.setEnvelopeIdentifier(message.getEnvelope().getIdentifier());
            existingLog.setLetterIdentifier(clinicalEmail.getLetter().getIdentifier());
        }

        refuseText = refuseText.replace("<Break/>", "\n").replace("<Break />", "\n").replace("<Break>", "\n").replace("< Break>", "\n").replace("</Break>", "").replace("</ Break>", "");
        existingLog.setNegativeReceiptReason(refuseText);
        existingLog.setReceiptXml(negativeXml);
        existingLog.setReceiptTts(now);
        existingLog.setReceiptS3FileKey(fileKey);
        existingLog.setReceiptType(ReceiptType.NEGATIVE);
        logService.save(existingLog);
    }

    private static String marshal(final Marshaller marshaller, final Object message) {
        final StringWriter sw = new StringWriter();
        try {
            marshaller.marshal(message, sw);
        } catch (JAXBException e) {
            log.error("Failed to parse Emessage to xml {}", e.getMessage());
        }
        return sw.toString();
    }

    private static String refuseCodeFromException(final Exception ex) {
        if (ex instanceof UnknownReceiverException) {
            return "ukendt_lokationsnummer";
        }
        return "ikke_specificeret";
    }

    private static String refuseTextFromException(final Exception ex) {
        if (ex instanceof UnknownReceiverException) {
            return "Modtager ikke fundet";
        }
        return "Ukendt fejl " + ex.getLocalizedMessage();
    }
}
