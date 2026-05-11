package dk.digitalidentity.medcommailbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.RecipientDao;
import dk.digitalidentity.medcommailbox.model.entity.Recipient;
import dk.digitalidentity.medcommailbox.model.entity.enums.IdentifierCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

	@Mock
	private RecipientDao recipientDao;
	@Mock
	private MailDao mailDao;

	@InjectMocks
	private RecipientService service;

	@Test
	void findByYdernummer_whenRecipientExists_returnsIt() {
		Recipient expected = new Recipient();
		when(recipientDao.findFirstByIdentifierAndIdentifierCodeOrderByIdAsc("012345", IdentifierCode.ydernummer))
				.thenReturn(Optional.of(expected));

		Optional<Recipient> result = service.findByYdernummer("012345");

		assertThat(result).isPresent().contains(expected);
	}

	@Test
	void findByYdernummer_whenNoRecipientExists_returnsEmpty() {
		when(recipientDao.findFirstByIdentifierAndIdentifierCodeOrderByIdAsc("999999", IdentifierCode.ydernummer))
				.thenReturn(Optional.empty());

		Optional<Recipient> result = service.findByYdernummer("999999");

		assertThat(result).isEmpty();
	}

	@Test
	void findByYdernummer_doesNotFallBackToEanLookup() {
		when(recipientDao.findFirstByIdentifierAndIdentifierCodeOrderByIdAsc("012345", IdentifierCode.ydernummer))
				.thenReturn(Optional.empty());

		Optional<Recipient> result = service.findByYdernummer("012345");

		assertThat(result).isEmpty();
		// findFirstByEanIdentifier must not be called — ydernummer lookup is exact, no fallback
		org.mockito.Mockito.verify(recipientDao, org.mockito.Mockito.never()).findFirstByEanIdentifier(org.mockito.ArgumentMatchers.any());
	}
}
