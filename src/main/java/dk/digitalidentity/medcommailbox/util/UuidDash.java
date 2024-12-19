package dk.digitalidentity.medcommailbox.util;

import org.apache.commons.lang3.StringUtils;

public class UuidDash {

    public static String removeDashes(final String uuid) {
        return uuid.replaceAll("-", "");
    }

    /**
     * Ensures that the uuid have dashed
     */
    public static String ensureDashes(final String uuid) {
        if (StringUtils.contains(uuid, "-")) {
            return uuid;
        }
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }

}
