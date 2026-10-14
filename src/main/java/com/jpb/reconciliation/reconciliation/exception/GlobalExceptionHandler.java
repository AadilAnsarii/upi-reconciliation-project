package com.jpb.reconciliation.reconciliation.exception;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.jpb.reconciliation.reconciliation.dto.ErrorResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Catch-all — was missing @ExceptionHandler, so it NEVER fired before ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception, WebRequest webRequest) {
        // Browser aborts connection mid-response (e.g. page redirect while image loading) — not a real error
        if (exception instanceof org.apache.catalina.connector.ClientAbortException
                || (exception.getCause() instanceof java.io.IOException && exception.getMessage() != null
                    && exception.getMessage().contains("aborted"))) {
            log.debug("Client aborted connection at [{}]", webRequest.getDescription(false));
            return null;
        }
        log.error("Unhandled exception at [{}]: {}", webRequest.getDescription(false), exception.getMessage(), exception);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again or contact support.",
                LocalDate.now());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Email delivery failure ────────────────────────────────────────────────
    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleEmailDeliveryException(EmailDeliveryException exception,
                                                                             WebRequest webRequest) {
        log.error("Email delivery failed to [{}] at [{}]: {}",
                exception.getRecipientEmail(), webRequest.getDescription(false), exception.getMessage(), exception);

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", "500");
        body.put("statusMsg",
                "We were unable to deliver the email. " +
                "Please verify the email address is correct and try again. " +
                "If the problem persists, contact support@kalinfotech.com");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Resource not found ────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException exception,
            WebRequest webRequest) {
        log.warn("Resource not found at [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(webRequest.getDescription(false), HttpStatus.NOT_FOUND,
                exception.getMessage(), LocalDate.now());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    // ── Resource already exists ───────────────────────────────────────────────
    @ExceptionHandler(ResourceAlreadyFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceFoundException(ResourceAlreadyFoundException exception,
            WebRequest webRequest) {
        log.warn("Resource conflict at [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(webRequest.getDescription(false),
                HttpStatus.FOUND, exception.getMessage(), LocalDate.now());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.FOUND);
    }

    // ── Bad credentials ───────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseDto> exceptionHandler(BadCredentialsException e) {
        log.warn("Bad credentials attempt: {}", e.getMessage());
        ResponseDto responseDto = new ResponseDto("400", "Invalid Username or Password !");
        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }

    // ── Disabled / unapproved user ────────────────────────────────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ResponseDto> exceptionUserHandler(DisabledException e) {
        log.warn("Disabled user login attempt: {}", e.getMessage());
        ResponseDto responseDto = new ResponseDto("400", "User is not approved.");
        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }
}
