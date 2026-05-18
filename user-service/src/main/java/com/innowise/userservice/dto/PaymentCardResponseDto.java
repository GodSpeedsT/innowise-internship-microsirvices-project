package com.innowise.userservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class PaymentCardResponseDto {

  private UUID cardId;
  private UUID userId;
  private String cardNumber;
  private String holder;
  private String expirationDate;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
