package dk.digitalidentity.medcommailbox.service.receivers.exceptions;

public class BadFileSizeException extends RuntimeException {
    public BadFileSizeException(String s) {
        super(s);
    }
}
