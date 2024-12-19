package dk.digitalidentity.medcommailbox.util;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

public class PrefixMappers {

    public static class Medcom2006PrefixMapper extends NamespacePrefixMapper {
        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (namespaceUri.equals("http://rep.oio.dk/sundcom.dk/medcom.dk/xml/schemas/2006/07/01/")) {
                return "";
            }
            return suggestion;
        }
    }

}
