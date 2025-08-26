package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.dao.model.enums.EpisodeOfCareStatusCode;
import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.session.LandingInfo;
import dk.digitalidentity.medcommailbox.session.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Objects;

@Controller
public class LandingController {

    public enum LandingArea {
        send_message
    }
    @Autowired
    private UserSession userSession;

    @RequireUserAccess
    @GetMapping("/landing")
    public String landing(@RequestParam(value = "sender_ean", required = false) final String senderEan,
                          @RequestParam(value = "receiver_ean", required = false) final String receiverEan,
                          @RequestParam(value = "patient_cpr", required = false) final String patientCpr,
                          @RequestParam(value = "patient_name", required = false) final String patientName,
                          @RequestParam(value = "subject", required = false) final String subject,
                          @RequestParam(value = "status", required = false) final String episodeOfCareStatus,
                          @RequestParam(value = "case_id", required = false) final String caseId,
                          @RequestParam("area") final LandingArea landingArea) {
        if (landingArea == LandingArea.send_message) {
            userSession.setLandingInfo(LandingInfo.builder()
                            .caseId(cleanupParam(caseId))
                            .patientName(cleanupParam(patientName))
                            .episodeOfCareStatusCode(episodeOfCareStatus != null ? EpisodeOfCareStatusCode.valueOf(cleanupParam(episodeOfCareStatus)) : null)
                            .patientCpr(cleanupCpr(patientCpr))
                            .receiverEan(cleanupParam(receiverEan))
                            .senderEan(cleanupParam(senderEan))
                            .subject(cleanupParam(subject))
                    .build());
            return "redirect:/mail/new";
        }
        return "redirect:/error";
    }

    record LandingInfoBody(LandingController.LandingArea area, String sender_ean, String receiver_ean, String patient_cpr, String patient_name,
                           String subject, String case_id, String episode_of_care_status) {}
    /**
     * This method will redirect the post request to a GET request on a secured endpoint, this ensures that parameters
     * are not lost when the user needs to login
     */
    @PostMapping(value = "/landing/form")
    public RedirectView landingPost(@SuppressWarnings("ClassEscapesDefinedScope") final LandingInfoBody body,
                                    final RedirectAttributes redirectAttributes) {
        if (body.area == LandingController.LandingArea.send_message) {
            redirectAttributes.addAttribute("area", body.area);
            if (body.case_id != null) {
                redirectAttributes.addAttribute("case_id", body.case_id);
            }
            if (body.patient_name != null) {
                redirectAttributes.addAttribute("patient_name", body.patient_name);
            }
            if (body.episode_of_care_status != null) {
                redirectAttributes.addAttribute("status", body.episode_of_care_status);
            }
            if (body.patient_cpr != null) {
                redirectAttributes.addAttribute("patient_cpr", body.patient_cpr);
            }
            if (body.receiver_ean != null) {
                redirectAttributes.addAttribute("receiver_ean", body.receiver_ean);
            }
            if (body.sender_ean != null) {
                redirectAttributes.addAttribute("sender_ean", body.sender_ean);
            }
            if (body.subject != null) {
                redirectAttributes.addAttribute("subject", body.subject);
            }
            return new RedirectView("/landing");
        }
        return new RedirectView("/error");
    }

    private static String cleanupCpr(final String value) {
        return StringUtils.getDigits(cleanupParam(value));
    }

    private static String cleanupParam(final String value) {
        if (Objects.equals(value, "undefined")) {
            return null;
        }
        return value;
    }
}
