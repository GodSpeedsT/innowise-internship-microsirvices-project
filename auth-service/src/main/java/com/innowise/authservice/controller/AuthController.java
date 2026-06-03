package com.innowise.authservice.controller;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.CredentialsRequest;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.TokenRequest;
import com.innowise.authservice.service.AuthService;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final JWKSet jwkSet;

  @PostMapping("/registrations")
  public ResponseEntity<Map<String, Object>> register(
      @Valid @RequestBody AuthRequest authRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of(
            "message", "User successfully registered",
            "userId", authService.register(authRequest)));
  }

  @PostMapping("/credentials")
  public ResponseEntity<Void> saveCredentials(
      @Valid @RequestBody CredentialsRequest credentialsRequest) {
    authService.saveUserCredentials(credentialsRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/tokens/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRequest request) {
    return ResponseEntity.ok(authService.refreshToken(request.getToken()));
  }

  @PostMapping("/tokens/validate")
  public ResponseEntity<Map<String, Object>> validate(
      @Valid @RequestBody TokenRequest request) {
    Jwt jwt = authService.validateToken(request.getToken());
    return ResponseEntity.ok(Map.of(
        "valid", true,
        "userId", jwt.getSubject(),
        "role", jwt.getClaim("role")
    ));
  }

  @GetMapping("/jwks.json")
  public ResponseEntity<Map<String, Object>> jwks() {
    return ResponseEntity.ok(jwkSet.toJSONObject());
  }

}
