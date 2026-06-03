package com.innowise.authservice.service;

import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.entity.Role;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

public interface TokenService {

  /**
   * Generates a signed JWT for the given subject.
   *
   * @param id        user UUID placed as the JWT subclaim
   * @param role      role placed as the role claim
   * @param isRefresh true to create a long-lived refresh token, false for a short-lived access
   *                  token
   * @return signed JWT string
   */
  String generateToken(UUID id, Role role, boolean isRefresh);

  /**
   * Decodes and validates a JWT string.
   *
   * @param token raw JWT string (without Bearer prefix)
   * @return the decoded Jwt
   * @throws JwtException if the token is invalid or expired
   */
  Jwt validateToken(String token);

  /**
   * Issues a new access + refresh token pair from a valid refresh token.
   *
   * @param refreshToken raw refresh JWT string
   * @return new AuthResponse with fresh tokens
   * @throws JwtException if the token is not a refresh token
   */
  AuthResponse refreshToken(String refreshToken);

}
