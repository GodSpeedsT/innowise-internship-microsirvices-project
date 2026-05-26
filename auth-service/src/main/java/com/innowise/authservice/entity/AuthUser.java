package com.innowise.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthUser {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "login", nullable = false, unique = true)
  private String login;
  @Column(name = "email", nullable = false)
  private String email;
  @Column(name = "password", nullable = false)
  private String password;
  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private Role role;
}
