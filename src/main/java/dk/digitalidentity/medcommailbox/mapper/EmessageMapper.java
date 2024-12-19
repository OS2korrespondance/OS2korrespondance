package dk.digitalidentity.medcommailbox.mapper;

import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.dao.BinaryDao;
import dk.digitalidentity.medcommailbox.dao.model.Binary;
import dk.digitalidentity.medcommailbox.dao.model.BinaryMessage;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.util.XmlUtil;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetter;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetterLetterType;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetterPatientType;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetterSenderType;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Receiver;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.BreakableText;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.CTRLReceiverType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.CTRLSenderType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeReceiptLetterType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.PositiveReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.PositiveReceiptLetterType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.AcknowledgementCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalEmail;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalEmailLetterType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalInformationNotSignedType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.DateTimeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.EnvelopeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.FormattedTextType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ObjectCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ObjectExtensionCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Patient;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ReceiverType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Reference;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.SenderNoContactType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.StatusCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.TypeCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.VersionCodeType;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.htmlToMedcomStyledText;
import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.toMedcom;
import static dk.digitalidentity.medcommailbox.mapper.MedcomMapper.toMedcom2012;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getBinaryLetter;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getClinicalEmail;
import static dk.digitalidentity.medcommailbox.util.NullSafe.nullSafe;
import static dk.digitalidentity.medcommailbox.util.UuidDash.ensureDashes;
import static dk.digitalidentity.medcommailbox.util.UuidDash.removeDashes;

/**
 * Mapper for mapping Emessages.
 * This class should be split into one for each namespace, its a bit cluttered as is.
 */
@Slf4j
@Component
public class EmessageMapper {

    @Autowired
    private Marshaller marshaller2006;
    @Autowired
    private Marshaller marshaller2012;
    @Autowired
    private BinaryDao binaryDao;
    @Autowired
    private S3Service s3Service;

    public record ToXmlResult(String envelopeIdentifier, String letterIdentifier, String xml) {};
    public ToXmlResult toXML(Emessage emessage) {
        if (emessage == null) {
            return null;
        }

        final String xml = marshal(marshaller2006, emessage);
        return new ToXmlResult(emessage.getEnvelope().getIdentifier(), getClinicalEmail(emessage).orElseThrow().getLetter().getIdentifier(), xml.replace("&lt;","<").replace("&gt;",">"));
    }

    public ToXmlResult binaryToXML(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage emessage) {
        if (emessage == null) {
            return null;
        }

        final String xml = marshal(marshaller2012, emessage);
        return new ToXmlResult(emessage.getEnvelope().getIdentifier(), getBinaryLetter(emessage).orElseThrow().getLetter().getIdentifier(), xml);
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

    public ObjectCodeType extensionToCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectExtensionCodeType objectExtensionCodeType) {
        return switch (objectExtensionCodeType) {
            case PCX, TIFF, JPEG, GIF, BMP, PNG, DCM -> ObjectCodeType.BILLEDE;
            case MPG, WAV, AVI, MID, RMI, INH, BIN -> ObjectCodeType.MULTIMEDIE;
            case SCP -> ObjectCodeType.BIOSIGNALER;
            case TXT, RTF, DOC, XLS, WPD -> ObjectCodeType.TEKSTFIL;
            case EXE, COM -> ObjectCodeType.PROGRAM;
            case PDF -> ObjectCodeType.VEKTOR_GRAFIK;
            case ZIP, PLO, FNX -> ObjectCodeType.PROPRIETAERT_INDHOLD;
        };
    }

    public dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage fromBinaryToMessage(final BinaryMessage binaryMessage,
                                                                                       final Sender configSender) {
        final DateTimeFormatter dateFormatterTime = DateTimeFormatter.ofPattern("HH:mm");
        final LocalDateTime now = LocalDateTime.now();
        final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage eMessage = new dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage();
        eMessage.setEnvelope(createEnvelopeBinary(now, dateFormatterTime));
        final BinaryLetter binaryLetter = new BinaryLetter();
        binaryLetter.setSender(createSenderBinary(configSender));
        binaryLetter.setLetter(createLetterBinary(now, dateFormatterTime));
        binaryLetter.setReceiver(createReceiver(binaryMessage));
        binaryLetter.setSystemInformation(false);
        binaryLetter.setPatient(createPatient(binaryMessage));
        binaryMessage.getBinaries().forEach(b -> {
            try {
                final byte[] content = s3Service.downloadBytes(b.getS3FileKey());
                BinaryLetter.BinaryObject object = new BinaryLetter.BinaryObject();
                object.setObjectCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectCodeType.fromValue(b.getCode()));
                object.setObjectExtensionCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectExtensionCodeType.fromValue(b.getExtensionCode()));
                object.setOriginalObjectSize(BigInteger.valueOf(b.getOriginalSize()));
                object.setObjectIdentifier(b.getIdentifier());
                object.setObjectBase64Encoded(Base64.getEncoder().encodeToString(content));
                binaryLetter.getBinaryObjects().add(object);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        eMessage.getHistopathologyReportsAndBinaryLettersAndLocalElements().add(binaryLetter);
        return eMessage;
    }

    public Emessage fromMailToEmessage(final Mail mail,
                                       final Sender configSender) {
        final DateTimeFormatter dateFormatterTime = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();

        Emessage eMessage = new Emessage();
        eMessage.setEnvelope(createEnvelopeClinical(now, dateFormatterTime));
        ClinicalEmail clinicalEmail = new ClinicalEmail();
        clinicalEmail.setLetter(createLetter(now, dateFormatterTime));
        clinicalEmail.setSender(createSender(configSender));
        ReceiverType receiver = createReceiver(mail);
        clinicalEmail.setReceiver(receiver);
        clinicalEmail.setPatient(createPatient(mail));
        clinicalEmail.setAdditionalInformation(createAdditionalInformation(mail));
        List<ClinicalInformationNotSignedType> clinicalInformationList = new ArrayList<>();

        final String content = htmlToMedcomStyledText(mail.getContent());

        List<String> contentSplit = XmlUtil.safeSplitText(content, 3150);
        for (String split : contentSplit) {
            FormattedTextType formattedTextType = new FormattedTextType();
            formattedTextType.getContent().add(split);
            ClinicalInformationNotSignedType clinicalInformation = new ClinicalInformationNotSignedType();
            clinicalInformation.setText01(formattedTextType);
            clinicalInformationList.add(clinicalInformation);
        }
        clinicalEmail.getClinicalInformations().addAll(clinicalInformationList);
        List<Reference> referenceList = new ArrayList<>();

        for(dk.digitalidentity.medcommailbox.dao.model.Reference mailRef : mail.getReferences()) {
            final Binary binary = binaryDao.findFirstByIdentifier(ensureDashes(mailRef.getObjectIdentifier()))
                    .orElseThrow(() -> new IllegalArgumentException("Could not find binary file"));
            Reference reference = new Reference();
            reference.setRefDescription(mailRef.getFilename());
            Reference.BIN bin = new Reference.BIN();
            for (ObjectExtensionCodeType type : ObjectExtensionCodeType.values()) {
                if (StringUtils.endsWithIgnoreCase(mailRef.getFilename(), type.value())) {
                    bin.setObjectExtensionCode(type);
                    break;
                }
            }
            final ObjectCodeType code = switch (bin.getObjectExtensionCode()) {
                case PCX, TIFF, JPEG, GIF, BMP, PNG, DCM -> ObjectCodeType.BILLEDE;
                case MPG, WAV, AVI, MID, RMI, INH, BIN -> ObjectCodeType.MULTIMEDIE;
                case SCP -> ObjectCodeType.BIOSIGNALER;
                case TXT, RTF, DOC, XLS, WPD -> ObjectCodeType.TEKSTFIL;
                case EXE, COM -> ObjectCodeType.PROGRAM;
                case PDF -> ObjectCodeType.VEKTOR_GRAFIK;
                case ZIP -> ObjectCodeType.PROPRIETAERT_INDHOLD;
            };
            bin.setObjectIdentifier(removeDashes(mailRef.getObjectIdentifier()));
            bin.setObjectCode(code);
            bin.setOriginalObjectSize(BigInteger.valueOf(mailRef.getObjectOriginalSize()));

            reference.setBIN(bin);
            referenceList.add(reference);
        }
        clinicalEmail.getReferences().addAll(referenceList);
        eMessage.getClinicalEmailsAndLaboratoryAnalysisFilesAndLaboratoryRequests().add(clinicalEmail);
        return eMessage;
    }

    public dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage createPositiveReceipt(final Emessage orgEMessage, final LocalDateTime now) {
        final ClinicalEmail clinicalEmail = getClinicalEmail(orgEMessage).orElseThrow();
        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage();
        eMessage.setEnvelope(createEnvelopeType2005(now));

        final PositiveReceipt positiveReceipt = createPositiveRecipt2005(clinicalEmail.getSender().getEANIdentifier(), clinicalEmail.getReceiver().getEANIdentifier());
        final PositiveReceipt.OriginalEmessage originalEmessage = createPositiveOriginalClinicalEmessage(orgEMessage);
        final PositiveReceipt.OriginalEmessage.OriginalLetter originalLetter = createPositiveOrginalLetter(clinicalEmail);
        originalEmessage.getOriginalLetters().add(originalLetter);
        positiveReceipt.setOriginalEmessage(originalEmessage);
        eMessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().add(positiveReceipt);
        return eMessage;
    }

    public dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage createPositiveReceiptForBinary(final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage orgEMessage, final LocalDateTime now) {
        final BinaryLetter binaryLetter = getBinaryLetter(orgEMessage).orElseThrow();
        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage();
        eMessage.setEnvelope(createEnvelopeType2005(now));

        final PositiveReceipt positiveReceipt = createPositiveRecipt2005(binaryLetter.getSender().getIdentifier(), binaryLetter.getReceiver().getEANIdentifier());
        final PositiveReceipt.OriginalEmessage originalEmessage = createPositiveOriginalBinaryEmessage(orgEMessage);
        final PositiveReceipt.OriginalEmessage.OriginalLetter originalLetter = createPositiveOrginalLetter(binaryLetter);
        originalEmessage.getOriginalLetters().add(originalLetter);
        positiveReceipt.setOriginalEmessage(originalEmessage);
        eMessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().add(positiveReceipt);
        return eMessage;
    }

    public dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage createNegativeReceiptForBinary(final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage orgEMessage,
                                                                                                             final LocalDateTime now,
                                                                                                             final String refuseText,
                                                                                                             final String refuseCode) {
        final BinaryLetter binaryLetter = getBinaryLetter(orgEMessage).orElseThrow();
        final DateTimeFormatter dateFormatterTime = DateTimeFormatter.ofPattern("HH:mm");

        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage();
        eMessage.setEnvelope(createNegativeEnvelope(now, dateFormatterTime));

        final NegativeReceipt negativeReceipt = createNegativeReceipt2005(binaryLetter.getSender().getEANIdentifier(), binaryLetter.getReceiver().getEANIdentifier());
        final NegativeReceipt.OriginalEmessage.OriginalLetter originalLetter = createNegativeOriginalLetter(
                binaryLetter.getLetter().getIdentifier(), nullSafe(() -> binaryLetter.getLetter().getVersionCode().value(), null), refuseText, refuseCode);
        final NegativeReceipt.OriginalEmessage originalEmessage = createNegativeOriginalMessage(
                orgEMessage.getEnvelope().getIdentifier(), binaryLetter.getSender().getEANIdentifier(), binaryLetter.getReceiver().getEANIdentifier());
        originalEmessage.getOriginalLetters().add(originalLetter);
        negativeReceipt.setOriginalEmessage(originalEmessage);

        eMessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().add(negativeReceipt);
        return eMessage;
    }

    public dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage createNegativeReceipt(final Emessage orgEMessage,
                                                                                                    final LocalDateTime now,
                                                                                                    final String refuseText,
                                                                                                    final String refuseCode) {
        final ClinicalEmail clinicalEmail = getClinicalEmail(orgEMessage).orElseThrow();
        final DateTimeFormatter dateFormatterTime = DateTimeFormatter.ofPattern("HH:mm");

        final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage eMessage = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage();
        eMessage.setEnvelope(createNegativeEnvelope(now, dateFormatterTime));

        final NegativeReceipt negativeReceipt = createNegativeReceipt2005(clinicalEmail.getSender().getEANIdentifier(), clinicalEmail.getReceiver().getEANIdentifier());
        final NegativeReceipt.OriginalEmessage.OriginalLetter originalLetter = createNegativeOriginalLetter(clinicalEmail.getLetter().getIdentifier(),
                clinicalEmail.getLetter().getVersionCode().value(), refuseText, refuseCode);
        final NegativeReceipt.OriginalEmessage originalEmessage = createNegativeOriginalMessage(
                orgEMessage.getEnvelope().getIdentifier(), clinicalEmail.getSender().getEANIdentifier(), clinicalEmail.getReceiver().getEANIdentifier());
        originalEmessage.getOriginalLetters().add(originalLetter);
        negativeReceipt.setOriginalEmessage(originalEmessage);

        eMessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().add(negativeReceipt);
        return eMessage;
    }

    private static PositiveReceipt createPositiveRecipt2005(final String senderEan, final String receiverEan) {
        PositiveReceipt positiveReceipt = new PositiveReceipt();
        positiveReceipt.setLetter(createPositiveLetter2005());
        CTRLSenderType sender = new CTRLSenderType();
        sender.setEANIdentifier(senderEan);
        positiveReceipt.setSender(sender);
        CTRLReceiverType receiver = new CTRLReceiverType();
        receiver.setEANIdentifier(receiverEan);
        positiveReceipt.setReceiver(receiver);
        return positiveReceipt;
    }

    private static PositiveReceipt.OriginalEmessage.OriginalLetter createPositiveOrginalLetter(ClinicalEmail clinicalEmail) {
        PositiveReceipt.OriginalEmessage.OriginalLetter originalLetter = new PositiveReceipt.OriginalEmessage.OriginalLetter();
        originalLetter.setOriginalLetterIdentifier(clinicalEmail.getLetter().getIdentifier());
        originalLetter.setOriginalVersionCode(clinicalEmail.getLetter().getVersionCode().value());
        return originalLetter;
    }

    private static PositiveReceipt.OriginalEmessage.OriginalLetter createPositiveOrginalLetter(BinaryLetter binaryLetter) {
        PositiveReceipt.OriginalEmessage.OriginalLetter originalLetter = new PositiveReceipt.OriginalEmessage.OriginalLetter();
        originalLetter.setOriginalLetterIdentifier(binaryLetter.getLetter().getIdentifier());
        originalLetter.setOriginalVersionCode(binaryLetter.getLetter().getVersionCode().value());
        return originalLetter;
    }

    private static dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType createEnvelopeType2005(final LocalDateTime now) {
        final DateTimeFormatter dateFormatterTime = DateTimeFormatter.ofPattern("HH:mm");
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.DateTimeType sent = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.DateTimeType();
        sent.setDate(now.toLocalDate());
        sent.setTime(now.format(dateFormatterTime));

        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType envelope = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType();
        envelope.setSent(sent);
        envelope.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        envelope.setAcknowledgementCode(dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.AcknowledgementCodeType.MINUSPOSITIVKVITT);
        return envelope;
    }

    private static PositiveReceiptLetterType createPositiveLetter2005() {
        PositiveReceiptLetterType letter = new PositiveReceiptLetterType();
        letter.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        letter.setVersionCode(dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.VersionCodeType.XC_0330_Q);
        letter.setStatisticalCode("XCTL03");
        return letter;
    }

    private static PositiveReceipt.OriginalEmessage createPositiveOriginalClinicalEmessage(final Emessage message) {
        final ClinicalEmail clinicalEmail = getClinicalEmail(message).orElseThrow();
        PositiveReceipt.OriginalEmessage originalEmessage = new PositiveReceipt.OriginalEmessage();
        originalEmessage.setOriginalEnvelopeIdentifier(message.getEnvelope().getIdentifier());
        CTRLSenderType originalSender = new CTRLSenderType();
        originalSender.setEANIdentifier(clinicalEmail.getSender().getEANIdentifier());
        originalEmessage.setOriginalSender(originalSender);
        CTRLReceiverType originalReceiver = new CTRLReceiverType();
        originalReceiver.setEANIdentifier(clinicalEmail.getReceiver().getEANIdentifier());
        originalEmessage.setOriginalReceiver(originalReceiver);
        return originalEmessage;
    }

    private static PositiveReceipt.OriginalEmessage createPositiveOriginalBinaryEmessage(final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage message) {
        final BinaryLetter binaryLetter = getBinaryLetter(message).orElseThrow();
        PositiveReceipt.OriginalEmessage originalEmessage = new PositiveReceipt.OriginalEmessage();
        originalEmessage.setOriginalEnvelopeIdentifier(message.getEnvelope().getIdentifier());
        CTRLSenderType originalSender = new CTRLSenderType();
        originalSender.setEANIdentifier(binaryLetter.getSender().getEANIdentifier());
        originalEmessage.setOriginalSender(originalSender);
        CTRLReceiverType originalReceiver = new CTRLReceiverType();
        originalReceiver.setEANIdentifier(binaryLetter.getReceiver().getEANIdentifier());
        originalEmessage.setOriginalReceiver(originalReceiver);
        return originalEmessage;
    }

    private static NegativeReceipt createNegativeReceipt2005(final String senderEan, final String receiverEan) {
        NegativeReceipt negativeReceipt = new NegativeReceipt();
        negativeReceipt.setLetter(createNegativeLetterType2005());
        CTRLSenderType sender = new CTRLSenderType();
        sender.setEANIdentifier(senderEan);
        negativeReceipt.setSender(sender);
        CTRLReceiverType receiver = new CTRLReceiverType();
        receiver.setEANIdentifier(receiverEan);
        negativeReceipt.setReceiver(receiver);
        return negativeReceipt;
    }

    private static NegativeReceipt.OriginalEmessage.OriginalLetter createNegativeOriginalLetter(final String letterIdentifier, final String originalVersionCode, final String refuseText, final String refuseCode) {
        NegativeReceipt.OriginalEmessage.OriginalLetter originalLetter = new NegativeReceipt.OriginalEmessage.OriginalLetter();
        originalLetter.setOriginalLetterIdentifier(letterIdentifier);
        originalLetter.setOriginalVersionCode(originalVersionCode);
        originalLetter.setRefuseCode(refuseCode);
        final BreakableText refuseBreakableText = new BreakableText();
        refuseBreakableText.getContent().add(refuseText);
        originalLetter.setRefuseText(refuseBreakableText);
        return originalLetter;
    }

    private static NegativeReceipt.OriginalEmessage createNegativeOriginalMessage(final String envelopeIdentifier,
                                                                                  final String senderEan, final String receiverEan) {
        NegativeReceipt.OriginalEmessage originalEmessage = new NegativeReceipt.OriginalEmessage();
        originalEmessage.setOriginalEnvelopeIdentifier(envelopeIdentifier);

        CTRLSenderType originalSender = new CTRLSenderType();
        originalSender.setEANIdentifier(senderEan);
        originalEmessage.setOriginalSender(originalSender);

        CTRLReceiverType originalReceiver = new CTRLReceiverType();
        originalReceiver.setEANIdentifier(receiverEan);
        originalEmessage.setOriginalReceiver(originalReceiver);
        return originalEmessage;
    }

    private static ClinicalEmail.AdditionalInformation createAdditionalInformation(Mail mail) {
        ClinicalEmail.AdditionalInformation additionalInformation = new ClinicalEmail.AdditionalInformation();
        if (mail.isHighPriority()) {
            additionalInformation.setPriority("høj_prioritet");
        } else {
            additionalInformation.setPriority("rutine");
        }
        additionalInformation.setSubject(mail.getSubject());
        return additionalInformation;
    }


    private static BinaryLetterPatientType createPatient(BinaryMessage binary) {
        BinaryLetterPatientType patient = new BinaryLetterPatientType();
        if (binary.getPatient().isAlternativeIdentifier()) {
            patient.setAlternativeIdentifier(binary.getPatient().getCpr());
        } else {
            patient.setCivilRegistrationNumber(binary.getPatient().getCpr());
        }
        String[] nameSplit = binary.getPatient().getName().split(" ");
        String surname = nameSplit[nameSplit.length-1];
        patient.setPersonSurnameName(surname);
        if (nameSplit.length > 1) {
            patient.setPersonGivenName(binary.getPatient().getName().replace(" " + surname, ""));
        }
        return patient;
    }

    private static Patient createPatient(Mail mail) {
        Patient patient = new Patient();
        if (mail.getPatient().isAlternativeIdentifier()) {
            patient.setAlternativeIdentifier(mail.getPatient().getCpr());
        } else {
            patient.setCivilRegistrationNumber(mail.getPatient().getCpr());
        }
        String[] nameSplit = mail.getPatient().getName().split(" ");
        String surname = nameSplit[nameSplit.length-1];
        patient.setPersonSurnameName(surname);
        if (nameSplit.length > 1) {
            patient.setPersonGivenName(mail.getPatient().getName().replace(" " + surname, ""));
        }
        patient.setEpisodeOfCareStatusCode(toMedcom(mail.getPatient().getEpisodeOfCareStatusCode()));
        return patient;
    }

    private static ReceiverType createReceiver(Mail mail) {
        ReceiverType receiver = new ReceiverType();
        receiver.setEANIdentifier(mail.getRecipient().getEanIdentifier());
        receiver.setIdentifier(mail.getRecipient().getIdentifier());
        receiver.setIdentifierCode(toMedcom(mail.getRecipient().getIdentifierCode()));
        receiver.setOrganisationName(StringUtils.truncate(mail.getRecipient().getShortOrganisationName(), 35));
        return receiver;
    }

    private static Receiver createReceiver(BinaryMessage binaryMessage) {
        Receiver receiver = new Receiver();
        receiver.setEANIdentifier(binaryMessage.getRecipient().getEanIdentifier());
        receiver.setIdentifier(binaryMessage.getRecipient().getIdentifier());
        receiver.setIdentifierCode(toMedcom2012(binaryMessage.getRecipient().getIdentifierCode()));
        receiver.setOrganisationName(StringUtils.truncate(binaryMessage.getRecipient().getShortOrganisationName(), 35));
        return receiver;
    }

    private static SenderNoContactType createSender(Sender configSender) {
        SenderNoContactType sender = new SenderNoContactType();
        sender.setEANIdentifier(configSender.getEanIdentifier());
        sender.setIdentifier(configSender.getIdentifier());
        sender.setIdentifierCode(toMedcom(configSender.getIdentifierCode()));
        sender.setOrganisationName(StringUtils.truncate(configSender.getOrganisationName(), 35));
        return sender;
    }

    private static BinaryLetterSenderType createSenderBinary(Sender configSender) {
        BinaryLetterSenderType sender = new BinaryLetterSenderType();
        sender.setEANIdentifier(configSender.getEanIdentifier());
        sender.setIdentifier(configSender.getIdentifier());
        sender.setIdentifierCode(toMedcom2012(configSender.getIdentifierCode()));
        sender.setOrganisationName(StringUtils.truncate(configSender.getOrganisationName(), 35));
        return sender;
    }

    private static ClinicalEmailLetterType createLetter(LocalDateTime now, DateTimeFormatter dateFormatterTime) {
        ClinicalEmailLetterType letter = new ClinicalEmailLetterType();
        letter.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        letter.setVersionCode(VersionCodeType.XD_9134_L);
        letter.setStatisticalCode("XDIS91");
        DateTimeType authorisation = new DateTimeType();
        authorisation.setDate(now.toLocalDate());
        authorisation.setTime(now.format(dateFormatterTime));
        letter.setAuthorisation(authorisation);
        letter.setTypeCode(TypeCodeType.XDIS_91);
        letter.setStatusCode(StatusCodeType.NYTBREV);
        return letter;
    }

    private static BinaryLetterLetterType createLetterBinary(LocalDateTime now, DateTimeFormatter dateFormatterTime) {
        BinaryLetterLetterType letter = new BinaryLetterLetterType();
        letter.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        letter.setVersionCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.VersionCodeType.XB_0131_X);
        letter.setStatisticalCode("XB0131X");
        dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.DateTimeType authorisation = new dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.DateTimeType();
        authorisation.setDate(now.toLocalDate());
        authorisation.setTime(now.format(dateFormatterTime));
        letter.setAuthorisation(authorisation);
        letter.setTypeCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.TypeCodeType.XBIN_01);
        letter.setStatusCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.StatusCodeType.NYTBREV);
        return letter;
    }

    private static EnvelopeType createEnvelopeClinical(LocalDateTime now, DateTimeFormatter dateFormatterTime) {
        EnvelopeType envelope = new EnvelopeType();
        DateTimeType sent = new DateTimeType();
        sent.setDate(now.toLocalDate());
        sent.setTime(now.format(dateFormatterTime));
        envelope.setSent(sent);
        envelope.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        envelope.setAcknowledgementCode(AcknowledgementCodeType.PLUSPOSITIVKVITT);
        return envelope;
    }

    private static dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.EnvelopeType createEnvelopeBinary(LocalDateTime now, DateTimeFormatter dateFormatterTime) {
        dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.EnvelopeType envelope = new dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.EnvelopeType();
        dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.DateTimeType sent = new dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.DateTimeType();
        sent.setDate(now.toLocalDate());
        sent.setTime(now.format(dateFormatterTime));
        envelope.setSent(sent);
        envelope.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        envelope.setAcknowledgementCode(dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.AcknowledgementCodeType.PLUSPOSITIVKVITT);
        return envelope;
    }

    private static dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType createNegativeEnvelope(LocalDateTime now, DateTimeFormatter dateFormatterTime) {
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType envelope = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.EnvelopeType();
        dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.DateTimeType sent = new dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.DateTimeType();
        sent.setDate(now.toLocalDate());
        sent.setTime(now.format(dateFormatterTime));
        envelope.setSent(sent);
        envelope.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        envelope.setAcknowledgementCode(dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.AcknowledgementCodeType.MINUSPOSITIVKVITT);
        return envelope;
    }

    private static NegativeReceiptLetterType createNegativeLetterType2005() {
        NegativeReceiptLetterType letter = new NegativeReceiptLetterType();
        letter.setIdentifier(RandomStringUtils.randomAlphanumeric(14));
        letter.setVersionCode(dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.VersionCodeType.XC_0230_Q);
        letter.setStatisticalCode("XCTL02");
        return letter;
    }


}
