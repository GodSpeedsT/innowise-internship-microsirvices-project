package com.innowise.orderservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "user_id", nullable = false)
  private UUID userId;
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus status;
  @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPrice;
  @Column(name = "deleted", nullable = false)
  private boolean deleted;
  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  @LastModifiedDate
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<OrderItem> orderItems = new ArrayList<>();

}

