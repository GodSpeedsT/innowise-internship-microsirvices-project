package com.innowise.authservice.service.impl;

import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.service.TokenService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

  @Value("${jwt.access-expiration-ms}")
  private long accessExpiration;
  @Value("${jwt.refresh-expiration-ms}")
  private long refreshExpiration;

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  public String generateToken(UUID id, Role role, boolean isRefresh) {
    Instant now = Instant.now();
    long expirationMs;
    if (isRefresh) {
      expirationMs = refreshExpiration;
    } else {
      expirationMs = accessExpiration;
    }

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
        .issuer("auth-service")
        .issuedAt(now)
        .expiresAt(now.plusMillis(expirationMs))
        .subject(id.toString())
        .claim("role", role.name())
        .claim("isRefresh", isRefresh)
        .build();

    return this.jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
  }

  public AuthResponse refreshToken(String token) {
    Jwt jwt = validateToken(token);

    Boolean isRefresh = jwt.getClaimAsBoolean("isRefresh");
    if (isRefresh == null || !isRefresh) {
      throw new JwtException("Invalid token type. Refresh token expected");
    }
    UUID id = UUID.fromString(jwt.getSubject());
    Role role = Role.valueOf(jwt.getClaim("role").toString());

    return AuthResponse.builder()
        .accessToken(generateToken(id, role, false))
        .refreshToken(generateToken(id, role, true))
        .build();
  }

  public Jwt validateToken(String token) {
    try {
      return jwtDecoder.decode(token);
    } catch (JwtException e) {
      throw new JwtException("Token validation failed: " + e.getMessage());
    }
  }

}
