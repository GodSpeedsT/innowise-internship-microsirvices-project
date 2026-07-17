package com.innowise.orderservice.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCompletedEvent {

  private UUID id;
  private UUID orderId;
  private BigDecimal billAmount;

}
