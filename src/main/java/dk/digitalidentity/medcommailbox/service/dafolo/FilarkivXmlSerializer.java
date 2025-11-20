package dk.digitalidentity.medcommailbox.service.dafolo;

import dk.digitalidentity.medcommailbox.service.dafolo.model.Filarkiv;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

/**
 * Utility for serializing Filarkiv objects to XML
 */
@Slf4j
@Component
public class FilarkivXmlSerializer {

	private final JAXBContext jaxbContext;

	public FilarkivXmlSerializer() throws JAXBException {
		this.jaxbContext = JAXBContext.newInstance(Filarkiv.class);
	}

	/**
	 * Serialize a Filarkiv object to XML string
	 *
	 * @param filarkiv the Filarkiv object to serialize
	 * @return XML string representation
	 * @throws JAXBException if serialization fails
	 */
	public String toXml(Filarkiv filarkiv) throws JAXBException {
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");

		StringWriter writer = new StringWriter();
		marshaller.marshal(filarkiv, writer);

		String xml = writer.toString();

		// Remove standalone="yes" if present
		xml = xml.replace(" standalone=\"yes\"", "");

		// Remove xsi:schemaLocation if present
		xml = xml.replaceAll("\\s+xsi:schemaLocation=\"[^\"]*\"", "");

		// Replace xmlns:xs with xmlns:xsd to match expected format
		xml = xml.replace("xmlns:xs=", "xmlns:xsd=");

		return xml;
	}

	/**
	 * Serialize a Filarkiv object to XML byte array
	 *
	 * @param filarkiv the Filarkiv object to serialize
	 * @return XML as byte array (UTF-8 encoded)
	 * @throws JAXBException if serialization fails
	 */
	public byte[] toXmlBytes(Filarkiv filarkiv) throws JAXBException {
		return toXml(filarkiv).getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

}