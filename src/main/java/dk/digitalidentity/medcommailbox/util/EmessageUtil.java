package dk.digitalidentity.medcommailbox.util;

import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.BinaryLetter;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.NegativeVansReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.PositiveReceipt;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.ClinicalEmail;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Emessage;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public interface EmessageUtil {

    static Optional<ClinicalEmail> getClinicalEmail(final Emessage emessage) {
        return emessage.getClinicalEmailsAndLaboratoryAnalysisFilesAndLaboratoryRequests().stream()
                .filter(e -> e instanceof ClinicalEmail)
                .map(ClinicalEmail.class::cast)
                .findFirst();
    }

    static Optional<PositiveReceipt> getPositiveReceipt(final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage emessage) {
        return emessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().stream()
                .filter(e -> e instanceof PositiveReceipt)
                .map(PositiveReceipt.class::cast)
                .findFirst();
    }

    static Optional<NegativeReceipt> getNegativeReceipt(final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage emessage) {
        return emessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().stream()
                .filter(e -> e instanceof NegativeReceipt)
                .map(NegativeReceipt.class::cast)
                .findFirst();
    }

    static Optional<NegativeVansReceipt> getNegativeVansReceipt(final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Emessage emessage) {
        return emessage.getHospitalReferralsAndDischargeLettersAndOutPatientDischargeLetters().stream()
                .filter(e -> e instanceof NegativeVansReceipt)
                .map(NegativeVansReceipt.class::cast)
                .findFirst();
    }

    static Optional<BinaryLetter> getBinaryLetter(final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.Emessage emessage) {
        return emessage.getHistopathologyReportsAndBinaryLettersAndLocalElements().stream()
                .filter(e -> e instanceof BinaryLetter)
                .map(BinaryLetter.class::cast)
                .findFirst();
    }

    static String makeValidUUID(String identifier) {
        if (StringUtils.countMatches(identifier, "-") < 4) {
            return java.util.UUID.fromString(
                    identifier.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")
            ).toString();
        }
        return identifier;
    }

}
