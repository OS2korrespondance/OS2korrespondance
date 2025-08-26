package dk.digitalidentity.medcommailbox.service.receivers.exceptions;

public class RecipientNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecipientNotFoundException(String message) {
		super(message);
	}
}
