package com.innowise.userservice.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationErrorResponse {
private String message;
private int status;
private LocalDateTime timestamp;
private Map<String, String> errors;
}
