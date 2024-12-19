package dk.digitalidentity.medcommailbox.mapper;

import dk.digitalidentity.medcommailbox.dao.model.Reference;
import dk.digitalidentity.medcommailbox.dao.model.enums.EpisodeOfCareStatusCode;
import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import dk.digitalidentity.medcommailbox.dao.model.enums.ReferenceType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.EpisodeOfCareStatusCode01Type;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.FixedFont;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.FormattedTextType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.IdentifierCodeType;
import dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.MedicalSpecialityCodeType;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class MedcomMapper {

    public static String htmlToMedcomStyledText(String content) {
        content = content.replace("\n", "<Break/>");
        content = content.replace("<br/>", "<Break/>").replace("<br>", "<Break/>");
        content = content.replace("<pre>", "<FixedFont>").replace("</pre>", "</FixedFont>");
        content = content.replace("<strong>", "<Bold>").replace("</strong>", "</Bold>");
        content = content.replace("<u>", "<Underline>").replace("</u>", "</Underline>");
        content = content.replace("<em>", "<Italic>").replace("</em>", "</Italic>");
        content = content.replace("&nbsp;", "<Space/>");
        content = content.replace("<p style=\"text-align: right;\">", "<Right>").replace("</p>", "</Right>");
        content = content.replace("<p style=\"text-align: center;\">", "<Center>").replace("</p>", "</Center>");
        return content;
    }

    public static Reference fromMedcom(final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Reference medcomReference) {
        final Reference reference = new Reference();
        reference.setReferenceDescription(medcomReference.getRefDescription());
        if (medcomReference.isSUP() != null && medcomReference.isSUP()) {
            reference.setReferenceType(ReferenceType.SUP);
        } else if (medcomReference.getBIN() != null) {
            final dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Reference.BIN bin = medcomReference.getBIN();
            reference.setReferenceType(ReferenceType.BIN);
            reference.setObjectCode(bin.getObjectCode().value());
            reference.setObjectIdentifier(bin.getObjectIdentifier());
            reference.setObjectOriginalSize(bin.getOriginalObjectSize().longValue());
            reference.setObjectExtensionCode(bin.getObjectExtensionCode().value());
        } else if (medcomReference.getURL() != null) {
            reference.setReferenceType(ReferenceType.URL);
            reference.setReferenceUrl(medcomReference.getURL());
        }
        return reference;
    }

    public static dk.digitalidentity.medcommailbox.dao.model.enums.MedicalSpecialityCodeType fromMedcom(final MedicalSpecialityCodeType medicalSpecialityCodeType) {
        return dk.digitalidentity.medcommailbox.dao.model.enums.MedicalSpecialityCodeType.valueOf(StringUtils.lowerCase(medicalSpecialityCodeType.name()));
    }

    public static IdentifierCode fromMedcom(final IdentifierCodeType identifierCodeType) {
        return switch (identifierCodeType) {
            case SYGEHUSAFDELINGSNUMMER -> IdentifierCode.sygehusafdelingsnummer;
            case YDERNUMMER -> IdentifierCode.ydernummer;
            case LOKATIONSNUMMER -> IdentifierCode.lokationsnummer;
            case KOMMUNENUMMER -> IdentifierCode.kommunenummer;
            case AMT -> IdentifierCode.amt;
            case SORKODE -> IdentifierCode.sorkode;
        };
    }

    public static IdentifierCodeType toMedcom(final IdentifierCode code) {
        return switch (code) {
            case sygehusafdelingsnummer -> IdentifierCodeType.SYGEHUSAFDELINGSNUMMER;
            case ydernummer -> IdentifierCodeType.YDERNUMMER;
            case lokationsnummer -> IdentifierCodeType.LOKATIONSNUMMER;
            case kommunenummer -> IdentifierCodeType.KOMMUNENUMMER;
            case amt -> IdentifierCodeType.AMT;
            case sorkode -> IdentifierCodeType.SORKODE;
        };
    }


    public static dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType toMedcom2012(final IdentifierCode code) {
        return switch (code) {
            case sygehusafdelingsnummer -> dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType.SYGEHUSAFDELINGSNUMMER;
            case ydernummer -> dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType.YDERNUMMER;
            case lokationsnummer -> dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType.LOKATIONSNUMMER;
            case kommunenummer -> dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType.KOMMUNENUMMER;
            case sorkode -> dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType.SORKODE;
            case amt -> throw new IllegalArgumentException("Identifier code for 'amt' no longer exists");
        };
    }



    public static IdentifierCode fromMedcom(final dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.IdentifierCodeType identifierCodeType) {
        return switch (identifierCodeType) {
            case SYGEHUSAFDELINGSNUMMER -> IdentifierCode.sygehusafdelingsnummer;
            case YDERNUMMER -> IdentifierCode.ydernummer;
            case LOKATIONSNUMMER -> IdentifierCode.lokationsnummer;
            case KOMMUNENUMMER -> IdentifierCode.kommunenummer;
            case SORKODE -> IdentifierCode.sorkode;
        };
    }

    public static EpisodeOfCareStatusCode fromMedcom(final EpisodeOfCareStatusCode01Type episode) {
        return switch (episode) {
            case INAKTIV -> EpisodeOfCareStatusCode.inaktiv;
            case INDLAGT -> EpisodeOfCareStatusCode.indlagt;
            case AMBULANT -> EpisodeOfCareStatusCode.ambulant;
            case DOED -> EpisodeOfCareStatusCode.doed;
            case AMBULANT_ROENTGEN -> EpisodeOfCareStatusCode.ambulant_roentgen;
        };
    }

    public static EpisodeOfCareStatusCode01Type toMedcom(final EpisodeOfCareStatusCode episode) {
        return switch (episode) {
            case inaktiv -> EpisodeOfCareStatusCode01Type.INAKTIV;
            case indlagt -> EpisodeOfCareStatusCode01Type.INDLAGT;
            case ambulant -> EpisodeOfCareStatusCode01Type.AMBULANT;
            case doed -> EpisodeOfCareStatusCode01Type.DOED;
            case ambulant_roentgen -> EpisodeOfCareStatusCode01Type.AMBULANT_ROENTGEN;
        };
    }


    public static String medcomFreeTextToHtml(final FormattedTextType formattedTextType) {
        if (formattedTextType == null) {
            return "";
        }
        return medcomFreeTextContentToHtml(formattedTextType.getContent());
    }

    public static String medcomFreeTextContentToHtml(final Object content) {
        return switch (content) {
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.BreakableText txt -> medcomFreeTextContentToHtml(txt.getContent());
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.BreakableText txt -> medcomFreeTextContentToHtml(txt.getContent());
            case List<?> list -> list.stream()
                    .map(MedcomMapper::medcomFreeTextContentToHtml)
                    .collect(Collectors.joining());
            case String s -> s;
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Space ignored -> "&nbsp;";
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Space ignored -> "&nbsp;";
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07.Break ignored -> "<br/>";
            case dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01.Break ignored -> "<br/>";
            case FixedFont fixedFont ->
                    "<pre>" + medcomFreeTextContentToHtml(fixedFont.getContent()) + "</pre>";
            case JAXBElement<?> element -> switch (element.getName().getLocalPart()) {
                case "Right" -> "<p style=\"text-align: right;\">" + medcomFreeTextContentToHtml(element.getValue()) + "</p>";
                case "Center" -> "<p style=\"text-align: center;\">" + medcomFreeTextContentToHtml(element.getValue()) + "</p>";
                case "Bold" -> "<strong>" + medcomFreeTextContentToHtml(element.getValue()) + "</strong>";
                case "Italic" -> "<em>" + medcomFreeTextContentToHtml(element.getValue()) + "</em>";
                case "Underline" -> "<u>" + medcomFreeTextContentToHtml(element.getValue()) + "</u>";
                default -> "";
            };
            case null -> "";
            default -> {
                log.error("Failed to map freetext, element class not understood {}", content.getClass().getName());
                yield "";
            }
        };
    }
}
