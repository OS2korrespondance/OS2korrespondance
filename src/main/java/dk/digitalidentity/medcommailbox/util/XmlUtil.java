package dk.digitalidentity.medcommailbox.util;

import java.util.ArrayList;
import java.util.List;

public class XmlUtil {

    /**
     * Split text into smaller chunks of chunkSize.
     * Will not split in the middle of a tag, insted it will split right before the tag.
     */
    public static List<String> safeSplitText(final String text, final int chunkSize) {
        final List<String> ret = new ArrayList<>();
        if (text.length() > chunkSize) {
            StringBuilder currentString = new StringBuilder();
            for (int i = 0; i < text.length(); ++i) {
                if (currentString.length() >= chunkSize) {
                    ret.add(currentString.toString());
                    currentString = new StringBuilder();
                }
                char c = text.charAt(i);
                if (c == '<') {
                    // This mumbo jumbo, will search forward for the end of the current tag, and check if the length
                    // exceed chunkSize, and if it does it will finish the current chunk and start a new
                    int j = i + 1;
                    while (j < text.length() && text.charAt(j) != '>') {
                        j++;
                    }
                    if (j == text.length()) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    if (currentString.length() + (j - i) >= chunkSize) {
                        ret.add(currentString.toString());
                        currentString = new StringBuilder();
                    }
                }
                currentString.append(c);
            }
            if (!currentString.isEmpty()) {
                ret.add(currentString.toString());
            }
        } else {
            ret.add(text);
        }
        return ret;
    }



}
