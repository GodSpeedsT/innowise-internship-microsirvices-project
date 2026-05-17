package com.innowise.userservice.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UserRequestDto {

  @NotBlank(message = "Name is required")
  @Size(max = 50)
  private String username;
  @NotBlank(message = "Surname is required")
  @Size(max = 50)
  private String surname;
  @Email(message = "Enter the valid email")
  private String email;
  @NotBlank(message = "Birth date is required")
  @Past
  private LocalDate birthDate;

}
