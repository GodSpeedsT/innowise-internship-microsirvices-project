package com.innowise.authservice.service.impl;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.entity.AuthUser;
import com.innowise.authservice.exception.UserException;
import com.innowise.authservice.repository.AuthRepository;
import com.innowise.authservice.service.AuthService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UUID register(AuthRequest authRequest) {
    if (findUserByEmailOrLogin(authRequest) == null) {
      UUID uuid = UUID.randomUUID();
      AuthUser authUser = AuthUser.builder()
          .id(uuid)
          .login(authRequest.getLogin())
          .password(passwordEncoder.encode(authRequest.getPassword()))
          .email(authRequest.getEmail())
          .role(authRequest.getRole())
          .build();

      authRepository.save(authUser);

      return uuid;
    }
    return null;
  }

  @Transactional
  public void login(AuthRequest authRequest) {
    AuthUser user = findUserByEmailOrLogin(authRequest);
    if (user == null) {
      throw new UserException("User not found");
    }
    if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
      throw new UserException("Wrong password");
    }

  }

  @Transactional
  public void logout() {

  }

  @Transactional
  public void saveUserCredentials(AuthRequest authRequest) {
    AuthUser authUser = findUserByEmailOrLogin(authRequest);
    authUser.setPassword(passwordEncoder.encode(authRequest.getPassword()));
    authRepository.save(authUser);
  }

  private AuthUser findUserByEmailOrLogin(AuthRequest authRequest) {
    String login = authRequest.getLogin();
    String email = authRequest.getEmail();
    return authRepository.findByEmailOrLogin(login, email)
        .orElseThrow(() -> new UserException("User not found"));
  }

}
