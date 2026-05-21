package com.innowise.userservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_active", columnList = "active")
})
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_id", unique = true, nullable = false, updatable = false)
  private UUID id;
  @Column(name = "username", unique = true, nullable = false)
  private String username;
  @Column(name = "surname", nullable = false)
  private String surname;
  @Column(name = "email", unique = true, nullable = false)
  private String email;
  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;
  @Column(name = "active", nullable = false)
  private boolean active = true;
  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  private LocalDateTime updatedAt;
  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @Builder.Default
  private List<PaymentCard> cards = new ArrayList<>();


}
