package com.innowise.userservice.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class UserCreateDto {

  @NotNull(message = "id is required")
  private UUID id;
  @NotBlank(message = "Name is required")
  @Size(max = 50)
  private String name;
  @NotBlank(message = "Surname is required")
  @Size(max = 50)
  private String surname;
  @NotBlank(message = "Email is required")
  @Email(message = "Enter the valid email")
  private String email;
  @NotNull(message = "Birth date is required")
  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;

}
