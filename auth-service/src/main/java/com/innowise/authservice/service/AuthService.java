package com.innowise.authservice.service;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.CredentialsRequest;
import com.innowise.authservice.dto.LoginRequest;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

  /**
   * Registers a new user: persists credentials and creates the profile in the user-service.
   *
   * @param authRequest full registration payload
   * @return the new user's UUID (also created in user-service)
   */
  UUID register(AuthRequest authRequest);

  /**
   * Saves credentials for an account whose profile already exists.
   *
   * @param request credential-only payload
   */
  void saveUserCredentials(CredentialsRequest request);

  /**
   * Authenticates the user and returns an access + refresh token pair.
   *
   * @param loginRequest login and password
   * @return AuthResponse with both tokens
   */
  AuthResponse login(LoginRequest loginRequest);

  /**
   * Issues a new token pair from a valid refresh token.
   *
   * @param refreshToken the raw refresh JWT string
   * @return new AuthResponse
   */
  AuthResponse refreshToken(String refreshToken);

  /**
   * Validates a JWT and returns its decoded claims.
   *
   * @param token the raw JWT string
   * @return decoded Jwt with subject and role claims
   */
  Jwt validateToken(String token);

}
