package dk.digitalidentity.medcommailbox.session;

import dk.digitalidentity.medcommailbox.dao.model.enums.EpisodeOfCareStatusCode;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LandingInfo {

    private String senderEan;
    private String receiverEan;
    private String patientCpr;
    private String patientName;
    private String subject;
    private String caseId;
    private EpisodeOfCareStatusCode episodeOfCareStatusCode;

}
