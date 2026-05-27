package com.innowise.authservice.service;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AuthService {

  AuthResponse login(String login, String password);

  void logout();

  UUID register(AuthRequest authRequest);

  void saveUserCredentials(AuthRequest authRequest);

}
