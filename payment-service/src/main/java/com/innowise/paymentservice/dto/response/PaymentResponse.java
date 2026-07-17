package com.innowise.paymentservice.dto.response;

import com.innowise.paymentservice.entity.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {

  private String id;
  private UUID orderId;
  private UUID userId;
  private Status status;
  private LocalDateTime timestamp;
  private BigDecimal paymentAmount;

}
