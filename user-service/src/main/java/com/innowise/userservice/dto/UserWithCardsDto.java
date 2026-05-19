package com.innowise.userservice.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@JsonPropertyOrder({ "uuid", "username", "surname", "email", "birthday", "active", "cards", "createdAt", "updatedAt" })
@Data
public class UserWithCardsDto {

  private UUID uuid;
  private String username;
  private String surname;
  private String email;
  private LocalDate birthday;
  private boolean active;
  private List<PaymentCardResponseDto> cards;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
