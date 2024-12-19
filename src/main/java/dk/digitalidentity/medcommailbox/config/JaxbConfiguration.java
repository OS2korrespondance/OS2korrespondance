package dk.digitalidentity.medcommailbox.config;

import dk.digitalidentity.medcommailbox.util.PrefixMappers;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaxbConfiguration {

    @Bean
    public Marshaller marshaller2006() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance("dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01:dk.digitalidentity.medcommailbox.controller.rest.korrespondance");
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "iso-8859-1");
        jaxbMarshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new PrefixMappers.Medcom2006PrefixMapper());
        return jaxbMarshaller;
    }

    @Bean
    public Marshaller marshaller2005() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance("dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07");
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "iso-8859-1");
        return jaxbMarshaller;
    }

    @Bean
    public Marshaller marshaller2012() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance("dk.oio.rep.medcom_dk.xml.schemas._2012._03._28");
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "iso-8859-1");
        return jaxbMarshaller;
    }

    @Bean
    public Unmarshaller unmarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance("dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2005._08._07:dk.oio.rep.sundcom_dk.medcom_dk.xml.schemas._2006._07._01:dk.oio.rep.medcom_dk.xml.schemas._2012._03._28");
        return jaxbContext.createUnmarshaller();
    }

}
