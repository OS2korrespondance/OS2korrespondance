package dk.digitalidentity.medcommailbox.controller.rest.korrespondance;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@AllArgsConstructor
@XmlType(name = "extensions", propOrder = {"caseId"}, namespace = "http://rep.oio.dk/sundcom.dk/medcom.dk/xml/schemas/2006/07/01/")
public class CaseIdWrapper implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;

    @XmlElement(namespace = "http://rep.oio.dk/sundcom.dk/medcom.dk/xml/schemas/2006/07/01/")
    private String caseId;
}
