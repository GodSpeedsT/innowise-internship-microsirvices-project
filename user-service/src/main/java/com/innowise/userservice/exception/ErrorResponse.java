package com.innowise.userservice.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatusCode;

@Data
@Builder
public class ErrorResponse {

  private String message;
  private int status;
  private LocalDateTime timestamp;

}
