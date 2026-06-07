package com.innowise.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequest {

  @NotNull(message = "UserId is required")
  private UUID userId;
  @NotEmpty(message = "Order must contain at least one item")
  @Valid
  private List<OrderItemRequest> orderItems;

}
