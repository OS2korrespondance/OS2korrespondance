package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.dao.model.FailedS3Key;
import dk.digitalidentity.medcommailbox.service.receivers.MedcomReceiver;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeVansReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.PositiveReceipt;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class MedcomReceiveService {
    @Autowired
    private Unmarshaller unmarshaller;
    @Autowired
    private FailedS3KeyService failedS3KeyService;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ReceiptHandler receiptService;
    @Autowired
    private List<MedcomReceiver> receivers;

    public void receive() {
        final List<String> s3Keys = s3Service.getFileKeysFromFolder(FolderConstants.FOLDER_IN);
        s3Keys.stream()
                .filter(s3Key -> s3Key.endsWith(".xml") || s3Key.endsWith(".xml.encrypted"))
                .filter(s3Key -> receivers.stream().noneMatch(r -> r.isHandled(s3Key)))
                .filter(s3Key -> failedS3KeyService.getByS3FileKey(s3Key) == null)
                .map(this::loadXml)
                .filter(Objects::nonNull)
                .forEach(medcomXml -> {
                    if (isReceipt(medcomXml)) {
                        receiptService.handleReceipt(medcomXml)
                                        .ifPresent(receiptResult -> {
                                            if (receivers.stream().noneMatch(r -> r.handleReceipt(receiptResult))) {
                                                failMedcomFile(medcomXml);
                                            } else {
                                                s3Service.delete(medcomXml.getS3Key());// cleanup
                                            }
                                        });
                    } else {
                        receivers.stream()
                                .filter(r -> r.supports(medcomXml))
                                .forEach(r -> handle(medcomXml, r));
                    }
                });
    }

    private void handle(MedcomReceiver.MedcomXml medcomXml, MedcomReceiver r) {
        try {
            r.handle(medcomXml);
            s3Service.delete(medcomXml.getS3Key());// cleanup
        } catch (final Exception ex) {
            log.error("Failed to process {}", medcomXml.getS3Key());
            r.negativeReceipt(medcomXml, ex);
        }
    }

    private void failMedcomFile(final MedcomReceiver.MedcomXml medcomXml) {
        FailedS3Key failedS3Key = new FailedS3Key();
        failedS3Key.setS3FileKey(medcomXml.getS3Key());
        failedS3KeyService.save(failedS3Key);
    }

    private boolean isReceipt(final MedcomReceiver.MedcomXml medcomXml) {
        // We know this is a receipt if it is an Emessage in the 2005 namespace
        if (medcomXml.getParsedValue() instanceof Emessage emessage) {
            for (Object obj : emessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters()) {
                if (obj instanceof NegativeReceipt || obj instanceof NegativeVansReceipt || obj instanceof PositiveReceipt) {
                    return true;
                }
            }
        }
        return false;
    }

    private MedcomReceiver.MedcomXml loadXml(final String s3Key) {
        final byte[] xml;
        try {
            xml = s3Service.downloadBytes(s3Key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            final Object unmarshalledObject = unmarshaller.unmarshal(new ByteArrayInputStream(xml));
            return MedcomReceiver.MedcomXml.builder()
                    .fileContents(xml)
                    .parsedValue(unmarshalledObject)
                    .s3Key(s3Key)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse xml to Emessage for file with key {}. Will send negative receipt if possible. Error: {}", s3Key, e.getMessage());
            FailedS3Key failed = new FailedS3Key();
            failed.setS3FileKey(s3Key);
            failedS3KeyService.save(failed);
            log.error("Failed to parse {} please take action", s3Key);
        }
        return null;
    }

}
