package dk.digitalidentity.medcommailbox.util;

import org.apache.commons.io.FileUtils;

public class ThymeleafFormatters {

    public static String readableFileSize(long size) {
        return FileUtils.byteCountToDisplaySize(size);
    }

}
