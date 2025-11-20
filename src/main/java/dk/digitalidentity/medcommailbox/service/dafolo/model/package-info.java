@XmlSchema(
		namespace = "http://www.kombit.dk/2022/01/01/",
		elementFormDefault = XmlNsForm.QUALIFIED,
		xmlns = {
				@XmlNs(prefix = "", namespaceURI = "http://www.kombit.dk/2022/01/01/"),
				@XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
				@XmlNs(prefix = "xsd", namespaceURI = "http://www.w3.org/2001/XMLSchema")
		}
)
package dk.digitalidentity.medcommailbox.service.dafolo.model;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;