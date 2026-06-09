package com.innowise.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponse {

  private UUID itemId;
  private String name;
  private BigDecimal price;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
