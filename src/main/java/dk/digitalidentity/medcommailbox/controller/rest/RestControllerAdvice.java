package dk.digitalidentity.medcommailbox.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ControllerAdvice(assignableTypes = {MailRestController.class, AuditLogRestController.class, AdminRestController.class})
public class RestControllerAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<?> handleInvalidRequestException(final ResponseStatusException ex) {
        return new ResponseEntity<>(ex.getReason(), ex.getStatusCode());
    }

}
