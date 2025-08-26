package dk.digitalidentity.medcommailbox.event;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
import dk.digitalidentity.medcommailbox.dao.model.InboxSubscriber;
import dk.digitalidentity.medcommailbox.service.EmailService;
import dk.digitalidentity.medcommailbox.service.InboxSubscriberService;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.SimpleMessageHandler;
import dk.digitalidentity.simple_queue.json.JsonSimpleMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

import static dk.digitalidentity.medcommailbox.event.Events.NOTIFICATION_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler implements SimpleMessageHandler {
	private final MedcomMailboxConfiguration config;
	private final EmailService emailService;
	private final MessageSource messageSource;
	private final InboxSubscriberService subscriberService;

	@Override
	public boolean handles(QueueMessage message) {
		return message.getQueue().equals(NOTIFICATION_QUEUE);
	}

	@Override
	public boolean handleMessage(QueueMessage message) {
		log.info("Received notification event: {}", message.getBody());
		final NotificationEvent event = JsonSimpleMessage.fromJson(message.getBody(), NotificationEvent.class);
		final Set<InboxSubscriber> subscribers = subscriberService.findForEan(event.getInboxEan());
		if (subscribers != null && !subscribers.isEmpty()) {
			final String organisationName = config.getSenders().stream()
					.filter(s -> s.getEanIdentifier().equals(event.getInboxEan()))
					.map(Sender::getOrganisationName)
					.findFirst().orElseThrow(() -> new RuntimeException("No organisation name found for inbox EAN " + event.getInboxEan()));

			final String subject = event.getNegative()
					? messageSource.getMessage("email.negative.title", null, Locale.ENGLISH)
					:  messageSource.getMessage("email.title", null, Locale.ENGLISH);
			final String emailMessage = event.getNegative()
					? messageSource.getMessage("email.negative.body", new String[]{organisationName, config.getMunicipality()}, Locale.ENGLISH)
			        : messageSource.getMessage("email.body", new String[]{ organisationName, config.getMunicipality() }, Locale.ENGLISH);
			for (InboxSubscriber subscriber : subscribers) {
				emailService.sendMessage(subscriber.getEmail(), subject, emailMessage);
			}
		}
		return true;
	}

	@Override
	public boolean handleFailedMessage(QueueMessage message, Exception exception) {
		return false;
	}
}
