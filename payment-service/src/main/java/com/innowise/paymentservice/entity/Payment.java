package com.innowise.paymentservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

  @Id
  private String id;
  @Indexed(name = "payment_order_id_idx", unique = true)
  private UUID orderId;
  @Indexed(name = "payment_user_id_idx")
  private UUID userId;
  @Indexed(name = "payment_status_idx")
  private Status status;
  @CreatedDate
  private LocalDateTime timestamp;
  private BigDecimal paymentAmount;
}
