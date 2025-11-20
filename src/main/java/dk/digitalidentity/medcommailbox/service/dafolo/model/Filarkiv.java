package dk.digitalidentity.medcommailbox.service.dafolo.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * Root element for the archive index.xml file
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Filarkiv", namespace = "http://www.kombit.dk/2022/01/01/")
@XmlType(propOrder = {"hoveddokument", "bilagListe"}, namespace = "http://www.kombit.dk/2022/01/01/")
public class Filarkiv {

	@XmlElement(name = "Hoveddokument", namespace = "http://www.kombit.dk/2022/01/01/")
	private Hoveddokument hoveddokument;

	@XmlElement(name = "BilagListe", namespace = "http://www.kombit.dk/2022/01/01/")
	private BilagListe bilagListe;

}