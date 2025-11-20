package dk.digitalidentity.medcommailbox.controller.api;

import dk.digitalidentity.medcommailbox.model.api.ErrorEO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice(basePackages = "dk.digitalidentity.medcommailbox.controller.api")
public class ApiExceptionHandler {

    @ResponseBody
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> responseStatusException(final ResponseStatusException ex, final WebRequest request) {
        final ErrorEO apiError = ErrorEO.builder()
            .status(BAD_REQUEST.value())
            .error(ex.getStatusCode().toString())
            .timestamp(OffsetDateTime.now())
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .message(ex.getReason())
            .build();
        return ResponseEntity.status(ex.getStatusCode()).body(apiError);
    }

}
