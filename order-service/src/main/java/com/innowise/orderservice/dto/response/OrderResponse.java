package com.innowise.orderservice.dto.response;

import com.innowise.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

  private UUID orderId;
  private OrderStatus status;
  private BigDecimal totalPrice;
  private UserResponse user;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
