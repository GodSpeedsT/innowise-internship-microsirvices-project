package com.innowise.authservice.controller;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @PostMapping("/login")
  public String login(@RequestParam String username, @RequestParam String password) {

  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRequest authRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "User successfully registered", "userId",
            authService.register(authRequest)));
  }

  @PostMapping("/refresh")
  public String refresh(@RequestParam String token) {

  }

  @PostMapping("/validate")
  public String validate(@RequestParam String token) {

  }

}
