package com.innowise.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

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
  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  private LocalDateTime updatedAt;

}
