package com.innowise.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDtoForUserService {

  @NotNull
  private UUID id;
  @NotBlank(message = "Name is required")
  @Size(max = 50)
  private String name;
  @NotBlank(message = "Surname is required")
  @Size(max = 50)
  private String surname;
  @NotBlank(message = "Email is required")
  @Email(message = "Enter a valid email")
  private String email;
  @NotNull(message = "Birth date is required")
  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;
}
