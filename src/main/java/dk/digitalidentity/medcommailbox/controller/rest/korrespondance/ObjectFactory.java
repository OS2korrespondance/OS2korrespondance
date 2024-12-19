package dk.digitalidentity.medcommailbox.controller.rest.korrespondance;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

    @XmlElementDecl(namespace = "http://rep.oio.dk/sundcom.dk/medcom.dk/xml/schemas/2006/07/01/", name = "Local_Elements")
    public JAXBElement<CaseIdWrapper> createWrapper(CaseIdWrapper wrapper) {
        return new JAXBElement<CaseIdWrapper>(new QName("http://rep.oio.dk/sundcom.dk/medcom.dk/xml/schemas/2006/07/01/", "Local_Elements"), CaseIdWrapper.class, wrapper);
    }

}
