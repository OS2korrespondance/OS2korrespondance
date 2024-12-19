package dk.digitalidentity.medcommailbox;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;

@Slf4j
public class ResourceResolver implements LSResourceResolver {
    private final String workingDirectory;

    public ResourceResolver(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        final var realPath = workingDirectory + systemId;
        final var resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(realPath);
        return new Input(publicId, "src/main/resources/" + realPath, resourceAsStream);
    }

    @Setter
    @Getter
    public static class Input implements LSInput {
        private String publicId;
        private String systemId;
        private final BufferedInputStream inputStream;

        public Input(final String publicId, final String sysId, final InputStream input) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.inputStream = new BufferedInputStream(input);
        }

        public String getBaseURI() {
            return null;
        }

        public InputStream getByteStream() {
            return null;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getStringData() {
            synchronized (inputStream) {
                try {
                    byte[] input = new byte[inputStream.available()];
                    //noinspection ResultOfMethodCallIgnored
                    inputStream.read(input);
                    return new String(input);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
        }

        public void setBaseURI(String baseURI) {
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setStringData(String stringData) {
        }

    }

}
