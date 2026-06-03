package com.innowise.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {

  @NotBlank(message = "Login is required")
  String login;
  @NotBlank(message = "Password is required")
  String password;

}
