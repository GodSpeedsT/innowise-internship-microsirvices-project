package com.innowise.authservice.service.impl;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.UserDtoForUserService;
import com.innowise.authservice.entity.AuthUser;
import com.innowise.authservice.exception.UserException;
import com.innowise.authservice.repository.AuthRepository;
import com.innowise.authservice.service.AuthService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;
  private final RestClient restClient;
  private final TokenService tokenService;

  @Value("${user-service.url}")
  private String userServiceUrl;

  @Override
  @Transactional
  public UUID register(AuthRequest authRequest) {
    if (authRepository.exitsByLoginOrEmail(authRequest.getLogin(), authRequest.getEmail())) {
      throw new UserException("User with this login or email already exists");
    }

    AuthUser authUser = AuthUser.builder()
        .login(authRequest.getLogin())
        .password(passwordEncoder.encode(authRequest.getPassword()))
        .email(authRequest.getEmail())
        .role(authRequest.getRole())
        .build();

    AuthUser savedUser = authRepository.save(authUser);
    UUID id = savedUser.getId();

    UserDtoForUserService dto = UserDtoForUserService.builder()
        .id(id)
        .name(authRequest.getName())
        .surname(authRequest.getSurname())
        .email(authRequest.getEmail())
        .birthDate(authRequest.getBirthDate())
        .build();

    try {
      restClient.post()
          .uri(userServiceUrl + "/api/v1/users")
          .contentType(MediaType.APPLICATION_JSON)
          .body(dto)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("User-service call failed during registration for login={}; rolling back",
          authRequest.getLogin(), e);
      authRepository.delete(savedUser);
      throw new UserException(
          "Registration failed: User Service is unavailable. " + e.getMessage());
    }
    return id;
  }

  @Override
  @Transactional
  public void saveUserCredentials(AuthRequest authRequest) {
    if (authRepository.exitsByLoginOrEmail(authRequest.getLogin(), authRequest.getEmail())) {
      throw new UserException("User with this login or email already exists");
    }

    AuthUser authUser = AuthUser.builder()
        .login(authRequest.getLogin())
        .password(passwordEncoder.encode(authRequest.getPassword()))
        .email(authRequest.getEmail())
        .role(authRequest.getRole())
        .build();

    authRepository.save(authUser);
    log.info("Credentials saved for login={}", authRequest.getLogin());
  }

  @Override
  @Transactional(readOnly = true)
  public AuthResponse login(String login, String password) {
    AuthUser authUser = authRepository.findByLogin(login)
        .orElseThrow(() -> new UserException("Invalid login or password"));
    if (!passwordEncoder.matches(password, authUser.getPassword())) {
      throw new UserException("Invalid login or password");
    }

    String accessToken = tokenService.generateToken(authUser.getId(), authUser.getRole(), false);
    String refreshToken = tokenService.generateToken(authUser.getId(), authUser.getRole(), true);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();

  }

  @Override
  public void logout() {

  }

  public void refreshToken() {

  }

  public void validateToken(Authentication authentication) {

  }
}
