package com.innowise.authservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.service.impl.TokenServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
  @Mock
  private JwtEncoder jwtEncoder;

  @Mock
  private JwtDecoder jwtDecoder;

  @InjectMocks
  private TokenServiceImpl tokenService;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(tokenService, "accessExpiration", 900_000L);
    ReflectionTestUtils.setField(tokenService, "refreshExpiration", 604_800_000L);
  }

  @Test
  void generateToken_Access_SetsCorrectClaims() {
    Jwt mockJwt = mock(Jwt.class);
    when(mockJwt.getTokenValue()).thenReturn("signed.access.token");
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    String token = tokenService.generateToken(userId, Role.USER, false);

    assertThat(token).isEqualTo("signed.access.token");

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(captor.capture());

    JwtClaimsSet claims = captor.getValue().getClaims();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.<String>getClaim("role")).isEqualTo("USER");
    assertThat(claims.<Boolean>getClaim("isRefresh")).isFalse();
  }

  @Test
  void generateToken_Refresh_SetsIsRefreshTrue() {
    Jwt mockJwt = mock(Jwt.class);
    when(mockJwt.getTokenValue()).thenReturn("signed.refresh.token");
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    tokenService.generateToken(userId, Role.ADMIN, true);

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(captor.capture());

    JwtClaimsSet claims = captor.getValue().getClaims();
    assertThat(claims.<Boolean>getClaim("isRefresh")).isTrue();
    assertThat(claims.<String>getClaim("role")).isEqualTo("ADMIN");
  }

  @Test
  void generateToken_AccessAndRefresh_HaveDifferentExpirations() {
    Jwt mockJwt = mock(Jwt.class);
    when(mockJwt.getTokenValue()).thenReturn("token");
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    tokenService.generateToken(userId, Role.USER, false);
    tokenService.generateToken(userId, Role.USER, true);

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder, times(2)).encode(captor.capture());

    var allClaims = captor.getAllValues();
    var accessExpiry  = allClaims.get(0).getClaims().getExpiresAt();
    var refreshExpiry = allClaims.get(1).getClaims().getExpiresAt();

    assertThat(refreshExpiry).isAfter(accessExpiry);
  }

  @Test
  void refreshToken_WithValidRefreshToken_ReturnsNewPair() {
    String rawRefresh = "valid.refresh.token";

    Jwt mockRefreshJwt = Jwt.withTokenValue(rawRefresh)
        .header("alg", "none")
        .subject(userId.toString())
        .claim("role", "USER")
        .claim("isRefresh", true)
        .build();

    Jwt mockAccessJwt = mock(Jwt.class);
    when(mockAccessJwt.getTokenValue()).thenReturn("new.access.token");
    Jwt mockNewRefreshJwt = mock(Jwt.class);
    when(mockNewRefreshJwt.getTokenValue()).thenReturn("new.refresh.token");

    when(jwtDecoder.decode(rawRefresh)).thenReturn(mockRefreshJwt);
    when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
        .thenReturn(mockAccessJwt)
        .thenReturn(mockNewRefreshJwt);

    AuthResponse response = tokenService.refreshToken(rawRefresh);

    assertThat(response.getAccessToken()).isEqualTo("new.access.token");
    assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
    verify(jwtEncoder, times(2)).encode(any(JwtEncoderParameters.class));
  }

  @Test
  void refreshToken_WithAccessTokenInstead_ThrowsJwtException() {
    String rawAccess = "some.access.token";

    Jwt mockAccessJwt = Jwt.withTokenValue(rawAccess)
        .header("alg", "none")
        .subject(userId.toString())
        .claim("role", "USER")
        .claim("isRefresh", false)
        .build();

    when(jwtDecoder.decode(rawAccess)).thenReturn(mockAccessJwt);

    assertThatThrownBy(() -> tokenService.refreshToken(rawAccess))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("Refresh token expected");
  }

  @Test
  void refreshToken_WithMissingIsRefreshClaim_ThrowsJwtException() {
    String rawToken = "token.without.claim";

    Jwt jwtWithoutClaim = Jwt.withTokenValue(rawToken)
        .header("alg", "none")
        .subject(userId.toString())
        .claim("role", "USER")
        .build();

    when(jwtDecoder.decode(rawToken)).thenReturn(jwtWithoutClaim);

    assertThatThrownBy(() -> tokenService.refreshToken(rawToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void validateToken_ValidToken_ReturnsDecodedJwt() {
    Jwt expected = Jwt.withTokenValue("valid.token")
        .header("alg", "none")
        .subject(userId.toString())
        .claim("role", "USER")
        .build();

    when(jwtDecoder.decode("valid.token")).thenReturn(expected);

    Jwt result = tokenService.validateToken("valid.token");

    assertThat(result).isSameAs(expected);
  }

  @Test
  void validateToken_InvalidToken_ThrowsJwtException() {
    when(jwtDecoder.decode("bad.token"))
        .thenThrow(new JwtException("Malformed token"));

    assertThatThrownBy(() -> tokenService.validateToken("bad.token"))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("Token validation failed");
  }
}
