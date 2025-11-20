package dk.digitalidentity.medcommailbox.service.dafolo.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for the list of attachments
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class BilagListe {

	@XmlElement(name = "Bilag", namespace = "http://www.kombit.dk/2022/01/01/")
	private List<Bilag> bilag = new ArrayList<>();

}
