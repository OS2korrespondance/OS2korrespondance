package dk.digitalidentity.medcommailbox.service.receivers;

import dk.digitalidentity.medcommailbox.service.ReceiptHandler;
import lombok.Builder;
import lombok.Getter;

public interface MedcomReceiver {

    @Getter
    @Builder
    class MedcomXml {
        private byte[] fileContents;
        private String s3Key;
        private Object parsedValue;
    }

    boolean isHandled(final String filekey);

    boolean supports(final MedcomXml object);

    void handle(final MedcomXml object);

    boolean handleReceipt(final ReceiptHandler.ReceiptResult receiptResult);

    void negativeReceipt(final MedcomXml object, final Exception ex);

}
