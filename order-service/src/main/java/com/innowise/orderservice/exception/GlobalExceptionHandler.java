package com.innowise.orderservice.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(buildError(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(DuplicateEntityException.class)
  public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntityException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(buildError(HttpStatus.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(buildError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value",
            (a, b) -> a
        ));
    ErrorResponse body = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Validation failed",
        LocalDateTime.now(),
        fieldErrors
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
      AuthorizationDeniedException e) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access denied",
            LocalDateTime.now(),
            null
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage()));
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
