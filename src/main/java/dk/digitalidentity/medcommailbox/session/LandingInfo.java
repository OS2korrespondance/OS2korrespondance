package dk.digitalidentity.medcommailbox.session;

import dk.digitalidentity.medcommailbox.model.entity.enums.EpisodeOfCareStatusCode;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Getter
public class LandingInfo implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

    private String senderEan;
    private String receiverEan;
    private String patientCpr;
    private String patientName;
    private String subject;
    private String caseId;
    private String groupId;
    private EpisodeOfCareStatusCode episodeOfCareStatusCode;

	@Override
	public String toString() {
		return "LandingInfo{" +
				"senderEan='" + senderEan + '\'' +
				", receiverEan='" + receiverEan + '\'' +
				", patientName='" + patientName + '\'' +
				", subject='" + subject + '\'' +
				", caseId='" + caseId + '\'' +
				", groupId='" + groupId + '\'' +
				", episodeOfCareStatusCode=" + episodeOfCareStatusCode +
				'}';
	}
}
