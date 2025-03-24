package dk.digitalidentity.medcommailbox.util;

import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectExtensionCodeType;
import org.springframework.http.MediaType;

public class MedcomContentType {

    public static MediaType contentTypeFor(final String objectExtensionCode) {
        return switch (ObjectExtensionCodeType.fromValue(objectExtensionCode)) {
            case PCX, PLO, INH, FNX, BIN, ZIP, COM, RMI, MID, AVI, WAV, EXE, XLS, WPD, DOC, RTF, SCP, DCM, MPG, BMP,
                 TIFF -> MediaType.APPLICATION_OCTET_STREAM;
            case JPEG -> MediaType.IMAGE_JPEG;
            case GIF -> MediaType.IMAGE_GIF;
            case PNG -> MediaType.IMAGE_PNG;
            case TXT -> MediaType.TEXT_PLAIN;
            case PDF -> MediaType.APPLICATION_PDF;
        };
    }

}
