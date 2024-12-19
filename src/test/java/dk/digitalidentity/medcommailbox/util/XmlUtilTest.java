package dk.digitalidentity.medcommailbox.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlUtilTest {

    @Test
    public void willNotSplitShortText() {
        // Given
        final String testText = "Maecenas a mi sed tortor scelerisque ultricies vitae at massa.";
        // When
        final List<String> result = XmlUtil.safeSplitText(testText, testText.length());
        // Then
        assertThat(result).hasSize(1).element(0).isEqualTo(testText);
    }

    @Test
    public void willNotSplitLongText() {
        // Given
        final String testText = "Maecenas a mi sed tortor scelerisque ultricies vitae at massa.";
        // When
        final List<String> result = XmlUtil.safeSplitText(testText, 10);
        // Then
        assertThat(result).hasSize(7).element(6).isEqualTo("a.");
    }

    @Test
    public void willNotSplitTags1() {
        final String testText = "Maecenas<Break/><Break/> a <Break/>mi sed <Break/>tortor scelerisque<Break/> ultricies vitae<Break/><Break/> at massa.";
        // When
        final List<String> result = XmlUtil.safeSplitText(testText, 10);
        // Then
        assertThat(result).hasSize(14).allMatch(s -> s.length() <= 10);
        assertThat(result.get(0)).isEqualTo("Maecenas");
        assertThat(result.get(1)).isEqualTo("<Break/>");
        assertThat(result.get(2)).isEqualTo("<Break/> a");
    }

    @Test
    public void willNotSplitTags2() {
        final String testText = "Ma<Break/>123<Break/>__1<Break/>mi sed <Break/>tortor scelerisque<Break/> ultricies vitae<Break/><Break/> at massa.";
        // When
        final List<String> result = XmlUtil.safeSplitText(testText, 10);
        // Then
        assertThat(result).hasSize(14).allMatch(s -> s.length() <= 10);
        assertThat(result.get(0)).isEqualTo("Ma<Break/>");
        assertThat(result.get(1)).isEqualTo("123");
        assertThat(result.get(2)).isEqualTo("<Break/>__");
    }

    @Test
    public void willFailOnBadXml() {
        final String testText = "Maecenas<Break/><Break/> a <Break/>mi sed <Break/>tortor scelerisque<Break/> ultricies vitae<Break/><Break/> at massa.<";
        // When
        assertThatThrownBy(() -> XmlUtil.safeSplitText(testText, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
