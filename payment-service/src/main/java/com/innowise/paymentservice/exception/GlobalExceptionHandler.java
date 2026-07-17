package com.innowise.paymentservice.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex) {
    log.error("Resource not found exception", ex);
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(buildError(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        ));
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
    log.error("External service exception: ", ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(buildError(
            HttpStatus.SERVICE_UNAVAILABLE,
            "External service unavailable"
        ));
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(RuntimeException ex) {
    log.error("Authorization error", ex);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(buildError(
            HttpStatus.FORBIDDEN,
            "Access denied"
        ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handeValidationError(MethodArgumentNotValidException ex) {
    log.error("Incorrect input exception", ex);
    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        fieldErrors.put(error.getField(), error.getDefaultMessage())
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            LocalDateTime.now(),
            fieldErrors
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(buildError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error"
        ));
  }

  private ErrorResponse buildError(HttpStatus status, String message) {
    return new ErrorResponse(status.value(), message, LocalDateTime.now(), null);
  }

  public record ErrorResponse(
      int status,
      String message,
      LocalDateTime timestamp,
      Map<String, String> fieldErrors
  ) {

  }

}
