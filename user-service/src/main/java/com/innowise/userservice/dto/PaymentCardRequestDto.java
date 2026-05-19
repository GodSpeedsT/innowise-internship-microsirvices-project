package com.innowise.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class PaymentCardRequestDto {

  @NotNull(message = "User ID is required")
  private UUID userId;
  @NotBlank(message = "Card number is required")
  @Size(min = 16, max = 16, message = "Every card must to have 16 digits")
  private String cardNumber;
  @NotBlank(message = "Holder name is required")
  @Size(max = 100)
  private String holder;
  @NotBlank(message = "Expiration date is required")
  @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Expiration date must be in MM/YY format")
  private String expirationDate;

}
