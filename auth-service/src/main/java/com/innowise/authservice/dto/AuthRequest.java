package com.innowise.authservice.dto;

import com.innowise.authservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

  @NotBlank(message = "Login cannot be empty")
  @Size(min = 2, max = 100)
  private String login;
  @NotBlank(message = "Password cannot be empty")
  @Size(min = 12, max = 100, message = "Password must be at least 6 characters long")
  private String password;
  @NotNull
  @Size(min = 2, max = 100)
  @Email(message = "Email is required")
  private String email;
  @NotNull(message = "Role must be specified")
  private Role role;


}
