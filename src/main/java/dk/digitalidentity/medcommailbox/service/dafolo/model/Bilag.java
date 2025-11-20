package dk.digitalidentity.medcommailbox.service.dafolo.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an attachment/bilag in the archive
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"filNavn", "filAlternativtNavn", "filMediaType", "filBeskrivelse"})
public class Bilag {

	@XmlElement(name = "FilNavn", namespace = "http://www.kombit.dk/2022/01/01/", required = true)
	private String filNavn;

	@XmlElement(name = "FilAlternativtNavn", namespace = "http://www.kombit.dk/2022/01/01/")
	private String filAlternativtNavn;

	@XmlElement(name = "FilMediaType", namespace = "http://www.kombit.dk/2022/01/01/", required = true)
	private String filMediaType;

	@XmlElement(name = "FilBeskrivelse", namespace = "http://www.kombit.dk/2022/01/01/")
	private String filBeskrivelse;

}
