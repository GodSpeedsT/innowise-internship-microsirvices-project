package com.innowise.authservice.service.impl;

import com.innowise.authservice.client.AuthClient;
import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.CredentialsRequest;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.UserDtoForUserService;
import com.innowise.authservice.entity.AuthUser;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.exception.CredentialsException;
import com.innowise.authservice.repository.AuthRepository;
import com.innowise.authservice.service.AuthService;
import com.innowise.authservice.service.TokenService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final AuthClient authClient;

  @Override
  @Transactional
  public UUID register(AuthRequest authRequest) {
    checkUser(authRequest.getLogin(), authRequest.getEmail());

    AuthUser authUser = buildUser(authRequest.getLogin(), authRequest.getPassword(),
        authRequest.getEmail(), authRequest.getRole());
    AuthUser savedUser = authRepository.save(authUser);
    UUID userId = savedUser.getId();

    UserDtoForUserService dto = UserDtoForUserService.builder()
        .id(userId)
        .name(authRequest.getName())
        .surname(authRequest.getSurname())
        .email(authRequest.getEmail())
        .birthDate(authRequest.getBirthDate())
        .build();

    authClient.sendUserData(dto);

    return userId;
  }

  @Transactional
  public void saveUserCredentials(CredentialsRequest request) {
    checkUser(request.getLogin(), request.getEmail());
    AuthUser authUser = buildUser(request.getLogin(), request.getPassword(),
        request.getEmail(), request.getRole());
    authRepository.save(authUser);
    log.info("Credentials saved for login={}", request.getLogin());
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest loginRequest) {
    AuthUser authUser = authRepository.findByLogin(loginRequest.getLogin())
        .orElseThrow(() -> new CredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(loginRequest.getPassword(), authUser.getPassword())) {
      throw new CredentialsException("Invalid credentials");
    }

    String accessToken = tokenService.generateToken(authUser.getId(), authUser.getRole(), false);
    String refreshToken = tokenService.generateToken(authUser.getId(), authUser.getRole(), true);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  public AuthResponse refreshToken(String refreshToken) {
    return tokenService.refreshToken(refreshToken);
  }

  public Jwt validateToken(String token) {
    return tokenService.validateToken(token);
  }

  private void checkUser(String login, String email) {
    if (authRepository.existsByLoginOrEmail(login, email)) {
      throw new CredentialsException("Duplicate credentials");
    }
  }

  private AuthUser buildUser(String login, String password, String email, Role role) {
    return AuthUser.builder()
        .login(login)
        .password(passwordEncoder.encode(password))
        .email(email)
        .role(role)
        .build();
  }

}
