package com.innowise.authservice.controller;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.service.AuthService;
import com.innowise.authservice.service.impl.TokenService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final TokenService tokenService;

  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(
      @Valid @RequestBody AuthRequest authRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of(
            "message", "User successfully registered",
            "userId", authService.register(authRequest)));
  }

  @PostMapping("/credentials")
  public ResponseEntity<Map<String, Object>> saveCredentials(
      @Valid @RequestBody AuthRequest authRequest) {
    authService.saveUserCredentials(authRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @RequestParam String login,
      @RequestParam String password) {
    return ResponseEntity.ok(authService.login(login, password));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@RequestParam String token) {
    return ResponseEntity.ok(tokenService.refreshToken(token));
  }

  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validate(@RequestParam String token) {
    Jwt jwt = tokenService.validateToken(token);
    return ResponseEntity.ok(Map.of(
        "valid", true,
        "userId", jwt.getSubject(),
        "role", jwt.getClaim("role")
    ));
  }
}
