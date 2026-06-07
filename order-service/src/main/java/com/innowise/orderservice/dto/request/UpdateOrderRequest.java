package com.innowise.orderservice.dto.request;

import com.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

  @NotNull(message = "Status is required")
  private OrderStatus status;

}
