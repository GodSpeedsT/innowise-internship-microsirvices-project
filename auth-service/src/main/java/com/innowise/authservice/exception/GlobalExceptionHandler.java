package com.innowise.authservice.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(LoginException.class)
  public ResponseEntity<ErrorResponse> handleLoginException(LoginException ex) {
    log.warn("Login conflict: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(build(HttpStatus.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(EmailException.class)
  public ResponseEntity<ErrorResponse> handleEmailException(EmailException ex) {
    log.warn("Email conflict: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(build(HttpStatus.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
    log.warn("User error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage()));
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex) {
    log.warn("Invalid token: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(build(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
    log.warn("JWT error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(build(HttpStatus.UNAUTHORIZED, "Token validation failed: " + ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(build(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value",
            (e1, e2) -> e1 + "; " + e2
        ));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
        "status", HttpStatus.BAD_REQUEST.value(),
        "message", "Validation failed",
        "errors", errors,
        "timestamp", LocalDateTime.now().toString()
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
  }


  private ErrorResponse build(HttpStatus status, String message) {
    return ErrorResponse.builder()
        .status(status.value())
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

}
