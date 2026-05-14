package com.innowise.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "payment_cards", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_card_number", columnList = "card_number"),
    @Index(name = "idx_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCard {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "card_id", nullable = false, unique = true, updatable = false)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  @Column(name = "card_number", nullable = false, unique = true)
  private String number;
  @Column(name = "holder", nullable = false)
  private String holder;
  @Column(name = "expiration_date", nullable = false)
  private String expirationDate;
  @Column(name = "active", nullable = false)
  private boolean active = true;
  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  private LocalDateTime updatedAt;

}
