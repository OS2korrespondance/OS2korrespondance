package dk.digitalidentity.medcommailbox.service.receivers;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.BinaryDao;
import dk.digitalidentity.medcommailbox.dao.BinaryMessageDao;
import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.model.Binary;
import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import dk.digitalidentity.medcommailbox.dao.model.FailedS3Key;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.dao.model.Recipient;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.dao.model.enums.ReceiptType;
import dk.digitalidentity.medcommailbox.mapper.EmessageMapper;
import dk.digitalidentity.medcommailbox.service.FailedS3KeyService;
import dk.digitalidentity.medcommailbox.service.MedcomLogService;
import dk.digitalidentity.medcommailbox.service.ReceiptHandler;
import dk.digitalidentity.medcommailbox.service.RecipientService;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.service.receivers.exceptions.BadFileSizeException;
import dk.digitalidentity.medcommailbox.service.receivers.exceptions.UnknownReceiverException;
import dk.digitalidentity.medcommailbox.util.EmessageUtil;
import dk.digitalidentity.medcommailbox.util.UuidDash;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.AcknowledgementCodeType;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetter;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.VersionCodeType;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.fromMedcom;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getBinaryLetter;

@Service
@Slf4j
public class BinaryReceiver implements MedcomReceiver {
    @Autowired
    private BinaryDao binaryDao;
    @Autowired
    private BinaryMessageDao binaryMessageDao;
    @Autowired
    private MedcomLogService logService;
    @Autowired
    private FailedS3KeyService failedS3KeyService;
    @Autowired
    private MedcomMailboxConfiguration config;
    @Autowired
    private RecipientService recipientService;
    @Autowired
    private EmessageMapper emessageMapper;
    @Autowired
    private Marshaller marshaller2005;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private MailDao mailDao;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public boolean isHandled(final String filekey) {
        return binaryDao.findBinariesByS3FileKey(filekey).isPresent();
    }

    @Override
    public boolean supports(final MedcomXml object) {
        return (object.getParsedValue() instanceof Emessage)
                && getBinaryLetter((Emessage)object.getParsedValue()).isPresent();
    }

    @Transactional
    @Override
    public void handle(final MedcomXml medcomXml) {
        if (!(medcomXml.getParsedValue() instanceof Emessage emessage))  {
            throw new IllegalArgumentException("Emessage object must be of type Emessage");
        }
        final BinaryLetter binaryLetter = getBinaryLetter(emessage).orElseThrow();
        final String s3Key = medcomXml.getS3Key();
        // we have a log on the identifiers already (= it has been received before)
        final String envelopeIdentifier = emessage.getEnvelope().getIdentifier();
        final String letterIdentifier = binaryLetter.getLetter().getIdentifier();
        MedcomLog existingLog = logService.getByIncommingTrueAndEnvelopeIdentifierAndLetterIdentifier(envelopeIdentifier, letterIdentifier).orElse(null);
        if (existingLog != null) {
            log.warn("Mail with envelope identifier {} and letter identifier {} has been loaded before. The s3 fileKey of this mail is {}. Will send new receipt if asked for.", envelopeIdentifier, letterIdentifier, s3Key);
            if (existingLog.getReceiptType() != null && existingLog.getReceiptType().equals(ReceiptType.POSITIVE)) {
                if (emessage.getEnvelope().getAcknowledgementCode().equals(AcknowledgementCodeType.PLUSPOSITIVKVITT)) {
                    log.info("Sending new positive receipt for with envelope identifier {} and letter identifier {}", envelopeIdentifier, letterIdentifier);
                    sendPositiveReceipt(emessage, existingLog, true);
                }
            } else if (existingLog.getReceiptType() != null && existingLog.getReceiptType().equals(ReceiptType.NEGATIVE)) {
                if (!binaryLetter.getLetter().getVersionCode().equals(VersionCodeType.XB_0131_X)) {
                    log.info("Sending new negative (wrong verison code) receipt for with envelope identifier {} and letter identifier {}", envelopeIdentifier, letterIdentifier);
                    sendWrongVersionNegativeReceipt(emessage, medcomXml.getFileContents(), existingLog);
                }
            }
            failMessage(s3Key);
            return;
        }
        if (!VersionCodeType.XB_0131_X.equals(binaryLetter.getLetter().getVersionCode())) {
            log.error("Mail with s3 key {} has the wrong versionCode. Will send negative receipt.", s3Key);
            failMessage(s3Key);
            sendWrongVersionNegativeReceipt(emessage, medcomXml.getFileContents(), null);
            return;
        }
        if (config.getSenders().stream().noneMatch(s -> s.getEanIdentifier().equals(binaryLetter.getReceiver().getEANIdentifier()))) {
            log.error("Mail with s3 key {} has a recipient identifier that are not configured on this instance: {}. Skipping this time. You have to update the configuration to receive this mail.", s3Key, binaryLetter.getReceiver().getIdentifier());
            negativeReceipt(medcomXml, new UnknownReceiverException());
            return;
        }
        final Recipient sender = recipientService.findByEanIdentifier(binaryLetter.getSender().getEANIdentifier())
                .orElseGet(() -> {
                    final Recipient s = new Recipient();
                    s.setEanIdentifier(binaryLetter.getSender().getEANIdentifier());
                    s.setIdentifier(binaryLetter.getSender().getIdentifier());
                    s.setIdentifierCode(fromMedcom(binaryLetter.getSender().getIdentifierCode()));
                    s.setShortOrganisationName(binaryLetter.getSender().getOrganisationName());
                    s.setFullOrganisationName(binaryLetter.getSender().getOrganisationName());
                    return recipientService.save(s);
                });
        String str = emessage.getEnvelope().getSent().getDate() + " " + emessage.getEnvelope().getSent().getTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

        final BinaryMessage binaryMessage = binaryMessageDao.save(new BinaryMessage());
        binaryMessage.setReceived(dateTime);
        binaryMessage.setSender(sender);
        binaryMessage.setIncomming(true);
        binaryMessage.setLetterIdentifier(letterIdentifier);
        binaryMessage.setEnvelopeIdentifier(envelopeIdentifier);
        binaryLetter.getBinaryObjects().forEach(obj -> {
            final Binary binary = new Binary();
            binary.setIdentifier(obj.getObjectIdentifier());
            long filesize = obj.getOriginalObjectSize().longValue();
            binary.setOriginalSize(filesize);
            binary.setCode(obj.getObjectCode().value());
            binary.setExtensionCode(obj.getObjectExtensionCode().value());
            final byte[] fileContent = Base64.getDecoder().decode(StringUtils.deleteWhitespace(obj.getObjectBase64Encoded()));
            // Check size and if wrong
            if (filesize != fileContent.length) {
                throw new BadFileSizeException("Forventede " + filesize + " bytes, men modtog: " + fileContent.length);
            }

            final String uploadedS3Key = s3Service.upload(FolderConstants.FOLDER_BIN, obj.getObjectIdentifier() + "." + obj.getObjectExtensionCode(), fileContent);
            binary.setS3FileKey(uploadedS3Key);
            binaryMessage.getBinaries().add(binary);
            Optional<Mail> mail = mailDao.findFirstByOriginalFolderAndReferencesObjectIdentifier(Folder.INBOX, UuidDash.removeDashes(obj.getObjectIdentifier()));
            mail.ifPresent(m -> m.setBinaryMessage(binaryMessage));
            binary.setMessage(binaryMessage);
        });

        final MedcomLog log = new MedcomLog();
        log.setMailTts(LocalDateTime.now());
        log.setEnvelopeIdentifier(envelopeIdentifier);
        log.setLetterIdentifier(letterIdentifier);
        log.setIncomming(true);

        if (emessage.getEnvelope().getAcknowledgementCode().equals(AcknowledgementCodeType.PLUSPOSITIVKVITT)) {
            sendPositiveReceipt(emessage, log, false);
        }
        logService.save(log);
    }

    private void failMessage(String s3Key) {
        FailedS3Key failed = new FailedS3Key();
        failed.setS3FileKey(s3Key);
        failedS3KeyService.save(failed);
    }

    @Override
    public boolean handleReceipt(final ReceiptHandler.ReceiptResult receiptResult) {
        return binaryMessageDao.findBinariesByEnvelopeIdentifierAndLetterIdentifier(receiptResult.getEnvelopeIdentifier(),
                receiptResult.getLetterIdentifier(), false).isPresent();
    }

    @Override
    public void negativeReceipt(final MedcomXml xml, final Exception ex) {
        if (!(xml.getParsedValue() instanceof Emessage emessage))  {
            log.error("Do not know what to do with {}", xml.getS3Key());
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        String refuseText = refuseTextFromException(ex);
        String refuseCode = refuseCodeFromException(ex);
        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage negReceipt =
                emessageMapper.createNegativeReceiptForBinary(emessage, now, refuseText, refuseCode);
        final String negativeXml = marshal(marshaller2005, negReceipt);
        final byte[] file = negativeXml.getBytes(StandardCharsets.ISO_8859_1);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        final String uuid = UUID.randomUUID().toString();
        final String fileKey = uuid+"_negative_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);

        MedcomLog log = new MedcomLog();
        log.setMailXml("");
        log.setMailTts(now);
        log.setIncomming(true);
        log.setReceiptType(ReceiptType.NEGATIVE);
        log.setEnvelopeIdentifier(emessage.getEnvelope().getIdentifier());
        log.setLetterIdentifier(EmessageUtil.getBinaryLetter(emessage).map(e -> e.getLetter().getIdentifier()).orElse(null));
        log.setNegativeReceiptReason(ex.getLocalizedMessage());
        log.setReceiptXml(negativeXml);
        log.setReceiptTts(now);
        log.setReceiptS3FileKey(fileKey);
        logService.save(log);

        FailedS3Key failedS3Key = new FailedS3Key();
        failedS3Key.setS3FileKey(xml.getS3Key());
        failedS3KeyService.save(failedS3Key);
    }

    private void sendPositiveReceipt(final Emessage message, final MedcomLog receiptLog, final boolean save) {
        final LocalDateTime now = LocalDateTime.now();
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = emessageMapper.createPositiveReceiptForBinary(message, now);
        final String xml = marshal(marshaller2005, eMessage);
        byte[] file = xml.getBytes(StandardCharsets.ISO_8859_1);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String uuid = UUID.randomUUID().toString();
        String fileKey = uuid+"_positive_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);
        receiptLog.setReceiptTts(now);
        receiptLog.setReceiptType(ReceiptType.POSITIVE);
        receiptLog.setReceiptXml(xml);
        receiptLog.setReceiptS3FileKey(fileKey);

        if (save) {
            logService.save(receiptLog);
        }
    }

    private void sendWrongVersionNegativeReceipt(Emessage message, byte[] xml, MedcomLog existingLog) {
        final LocalDateTime now = LocalDateTime.now();
        final BinaryLetter binaryLetter = getBinaryLetter(message).orElseThrow();
        final String refuseText = "Brev med nummeret " + binaryLetter.getLetter().getIdentifier() + ", afsendt " + message.getEnvelope().getSent().getDate() + " kl. " + message.getEnvelope().getSent().getTime() + " kunne ikke modtages på grund af forkert versionsnummer.<Break/>Med venlig hilsen<Break/>Fælles forløb";
        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = emessageMapper.createNegativeReceiptForBinary(message, now, refuseText, "problem_med_version");
        final String negativeXml = marshal(marshaller2005, eMessage);
        final byte[] file = negativeXml.getBytes(StandardCharsets.ISO_8859_1);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_mm_dd_HH_mm_ss");
        final String uuid = UUID.randomUUID().toString();
        final String fileKey = uuid+"_negative_receipt_"+now.format(dateFormatter)+".xml";
        s3Service.upload(FolderConstants.FOLDER_OUT, fileKey, file);

        if (existingLog == null) {
            existingLog = new MedcomLog();
            existingLog.setMailXml("");
            existingLog.setMailTts(now);
            existingLog.setIncomming(true);
            existingLog.setReceiptType(ReceiptType.NEGATIVE);
            existingLog.setEnvelopeIdentifier(message.getEnvelope().getIdentifier());
            existingLog.setLetterIdentifier(binaryLetter.getLetter().getIdentifier());
        }

        final String normalRefuseText = refuseText.replace("<Break/>", "\n").replace("<Break />", "\n").replace("<Break>", "\n").replace("< Break>", "\n").replace("</Break>", "").replace("</ Break>", "");
        existingLog.setNegativeReceiptReason(normalRefuseText);
        existingLog.setReceiptXml(negativeXml);
        existingLog.setReceiptTts(now);
        existingLog.setReceiptType(ReceiptType.NEGATIVE);
        existingLog.setReceiptS3FileKey(fileKey);
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
        if (ex instanceof BadFileSizeException) {
            return ex.getMessage();
        }
        return "Ukendt fejl " + ex.getLocalizedMessage();
    }

}
