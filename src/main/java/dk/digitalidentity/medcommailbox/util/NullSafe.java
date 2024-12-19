package dk.digitalidentity.medcommailbox.util;

import java.util.function.Supplier;

public class NullSafe {

    public static <T> T nullSafe(Supplier<T> method, T defaultValue) {
        try {
            return method.get();
        } catch (final NullPointerException npe) {
            return defaultValue;
        }
    }

}
