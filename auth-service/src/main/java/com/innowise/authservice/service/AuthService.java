package com.innowise.authservice.service;

import com.innowise.authservice.dto.AuthRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AuthService {

  void login(String login, String password);
  void logout();
  UUID register(AuthRequest authRequest);
  void refreshToken();
  void validateToken(Authentication authentication);
  void saveUserCredentials(String login, String password);

}
