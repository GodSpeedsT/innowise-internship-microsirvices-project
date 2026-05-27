package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class UserResponseDto {

  private UUID uuid;
  private String name;
  private String surname;
  private String email;
  private LocalDate birthday;
  private boolean active;
  private List<PaymentCardResponseDto> cards;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
