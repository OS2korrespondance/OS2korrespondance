package dk.digitalidentity.medcommailbox.controller.mvc;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class LoginFailureControllerAdvice {

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseBody
    public String handleFailedLogin(final ResponseStatusException ex) {
        return "redirect:/error-login";
    }

}
