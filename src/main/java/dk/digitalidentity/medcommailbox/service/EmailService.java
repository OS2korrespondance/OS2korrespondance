package dk.digitalidentity.medcommailbox.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

	@Autowired
	private MedcomMailboxConfiguration configuration;

	public boolean sendMessage(String email, String subject, String message) {
		Transport transport = null;

		try {
			Properties props = System.getProperties();
			props.put("mail.transport.protocol", "smtps");
			props.put("mail.smtps.port", 465);
			props.put("mail.smtps.auth", "true");
			props.put("mail.smtps.starttls.enable", "true");
			props.put("mail.smtps.starttls.required", "true");
			Session session = Session.getDefaultInstance(props);

			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(configuration.getEmail().getFrom(),
					configuration.getEmail().getFrom()));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
			msg.setSubject(subject, "UTF-8");
			msg.setText(message, "UTF-8");
			msg.setHeader("Content-Type", "text/html; charset=UTF-8");

			transport = session.getTransport();
			transport.connect(configuration.getEmail().getHost(),
					configuration.getEmail().getUsername(),
					configuration.getEmail().getPassword());
			transport.addTransportListener(new TransportErrorHandler());
			transport.sendMessage(msg, msg.getAllRecipients());
		} catch (Exception ex) {
			log.error("Failed to send email to '" + email + "'", ex);

			return false;
		} finally {
			try {
				if (transport != null) {
					transport.close();
				}
			} catch (Exception ex) {
				log.warn("Error occured while trying to terminate connection", ex);
			}
		}

		return true;
	}

}