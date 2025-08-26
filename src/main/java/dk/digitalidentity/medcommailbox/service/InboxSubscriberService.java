package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.InboxDao;
import dk.digitalidentity.medcommailbox.dao.InboxSubscriberDao;
import dk.digitalidentity.medcommailbox.dao.model.Inbox;
import dk.digitalidentity.medcommailbox.dao.model.InboxSubscriber;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InboxSubscriberService {
	private final InboxSubscriberDao subscriberDao;
	private final InboxDao inboxDao;

	@Transactional
	public Inbox getOrCreateInbox(String ean) {
		return inboxDao.findByEanIdentifier(ean)
				.orElseGet(() -> inboxDao.save(Inbox.builder().negativeReceiptNotification(true).eanIdentifier(ean).build()));
	}

	@Transactional
	public Set<InboxSubscriber> findForEan(final String ean) {
		final Inbox inbox = getOrCreateInbox(ean);
		return new HashSet<>(inbox.getSubscribers());
	}

	/**
	 * Adds a new subscriber with the specified inbox EAN and email address.
	 */
	@Transactional
	public void addSubscriber(final String inboxEan, final String email) {
		final Inbox inbox = getOrCreateInbox(inboxEan);
		final InboxSubscriber subscriber = new InboxSubscriber();
		subscriber.setInbox(inbox);
		subscriber.setEmail(email);
		inbox.getSubscribers().add(subscriber);
		subscriberDao.save(subscriber);
	}

	/**
	 * Removes a subscriber identified by the specified inbox EAN and email address.
	 * If the subscriber does not exist, a {@link ResponseStatusException}
	 * with a status of 404 (Not Found) is thrown.
	 */
	@Transactional
	public void removeSubscriber(final String inboxEan, final String email) {
		final Inbox inbox = getOrCreateInbox(inboxEan);
		inbox.getSubscribers().removeIf(s -> s.getEmail().equals(email));
	}

	/**
	 * Retrieves a map of all subscribers, where the keys are their inbox EANs and the values are the corresponding
	 * Subscriber objects.
	 */
	public Map<String, List<InboxSubscriber>> mapOfSubscribers() {
		final List<InboxSubscriber> allSubscribers = subscriberDao.findAll();
		return allSubscribers.stream()
				.collect(java.util.stream.Collectors.toMap(s -> s.getInbox().getEanIdentifier(), Collections::singletonList,
						(a, b) -> java.util.stream.Stream.concat(a.stream(), b.stream())
								.sorted(java.util.Comparator.comparing(InboxSubscriber::getEmail))
								.collect(java.util.stream.Collectors.toList())));

	}

}
