package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class PaymentCardResponseDto {

  private UUID cardId;
  private UUID userUd;
  private String cardNumber;
  private String holder;
  private String expirationDate;
  private boolean active;
  private LocalDate createdAt;
  private LocalDate updatedAt;

}
