package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.model.FailedS3Key;
import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.dao.model.enums.ReceiptType;
import dk.digitalidentity.medcommailbox.mapper.MedcomMapper;
import dk.digitalidentity.medcommailbox.service.receivers.MedcomReceiver;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeVansReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.PositiveReceipt;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getNegativeReceipt;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getNegativeVansReceipt;
import static dk.digitalidentity.medcommailbox.util.EmessageUtil.getPositiveReceipt;

@Slf4j
@Service
public class ReceiptHandler {
    @Autowired
    private MedcomLogService logService;
    @Autowired
    private FailedS3KeyService failedS3KeyService;
    @Autowired
    private S3Service s3Service;

    @Getter
    @Setter
    public static class ReceiptResult {
        private String refuseText;
        private String envelopeIdentifier;
        private String letterIdentifier;
        private ReceiptType receiptType;
    }

    @Transactional
    public Optional<ReceiptResult> handleReceipt(final MedcomReceiver.MedcomXml medcomXml) {
        final Emessage receipt = (Emessage) medcomXml.getParsedValue();
        final ReceiptResult result = new ReceiptResult();
        getPositiveReceipt(receipt).ifPresent(positiveReceipt -> {
            final PositiveReceipt.OriginalEmessage originalEmessage = positiveReceipt.getOriginalEmessage();
            result.setReceiptType(ReceiptType.POSITIVE);
            result.setEnvelopeIdentifier(originalEmessage.getOriginalEnvelopeIdentifier());
            result.setLetterIdentifier(originalEmessage.getOriginalLetters().getFirst().getOriginalLetterIdentifier());
        });
        getNegativeReceipt(receipt).ifPresent(negativeReceipt -> {
            final NegativeReceipt.OriginalEmessage originalEmessage = negativeReceipt.getOriginalEmessage();
            result.setReceiptType(ReceiptType.NEGATIVE);
            result.setEnvelopeIdentifier(originalEmessage.getOriginalEnvelopeIdentifier());
            result.setLetterIdentifier(originalEmessage.getOriginalLetters().getFirst().getOriginalLetterIdentifier());
            result.setRefuseText(MedcomMapper.medcomFreeTextContentToHtml(originalEmessage.getRefuseText() == null
                    ? originalEmessage.getOriginalLetters().getFirst().getRefuseText()
                    : originalEmessage.getRefuseText()));
        });
        getNegativeVansReceipt(receipt).ifPresent(negativeVansReceipt -> {
            final NegativeVansReceipt.OriginalEmessage originalEmessage = negativeVansReceipt.getOriginalEmessage();
            result.setReceiptType(ReceiptType.NEGATIVE_VANS);
            result.setEnvelopeIdentifier(originalEmessage.getOriginalEnvelopeIdentifier());
            result.setLetterIdentifier(originalEmessage.getOriginalLetters().getFirst().getOriginalLetterIdentifier());
            result.setRefuseText(MedcomMapper.medcomFreeTextContentToHtml(originalEmessage.getRefuseText() == null
                    ? originalEmessage.getOriginalLetters().getFirst().getRefuseText()
                    : originalEmessage.getRefuseText()));
        });
        if (result.getReceiptType() == null) {
            throw new RuntimeException("Tried to handle receipt with S3 file Key: " + medcomXml.getS3Key() + ", but positive, negative and negative vans receipt was null.");
        }

        final MedcomLog exitingLog = logService.getByEnvelopeIdentifierAndLetterIdentifier(result.getEnvelopeIdentifier(), result.getLetterIdentifier())
                .orElseGet(MedcomLog::new);
        if (ReceiptType.NEGATIVE.equals(exitingLog.getReceiptType())) {
            FailedS3Key failedS3Key = new FailedS3Key();
            failedS3Key.setS3FileKey(medcomXml.getS3Key());
            failedS3KeyService.save(failedS3Key);
            log.warn("Handling receipt with s3Key {}. The mail that it is referring to has already received a negative receipt. Therefore this receipt will not be stored.", medcomXml.getS3Key());
            return Optional.empty();
        }
        exitingLog.setReceiptTts(LocalDateTime.now());
        exitingLog.setReceiptType(result.getReceiptType());
        exitingLog.setEnvelopeIdentifier(result.getEnvelopeIdentifier());
        exitingLog.setLetterIdentifier(result.getLetterIdentifier());
        exitingLog.setReceiptS3FileKey(medcomXml.getS3Key());
        exitingLog.setReceiptXml(new String(medcomXml.getFileContents(), StandardCharsets.ISO_8859_1));
        exitingLog.setReceiptType(result.getReceiptType());
        exitingLog.setNegativeReceiptReason(result.getRefuseText());
        logService.save(exitingLog);
        return Optional.of(result);
    }

}
