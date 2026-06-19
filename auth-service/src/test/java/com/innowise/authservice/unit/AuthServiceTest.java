package com.innowise.authservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.innowise.authservice.service.TokenService;
import com.innowise.authservice.service.impl.AuthServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthRepository authRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private TokenService tokenService;
  @Mock
  private AuthClient authClient;

  @InjectMocks
  private AuthServiceImpl authService;

  @Test
  void register_Success_ShouldSaveUserAndCallUserService() {
    AuthRequest request = validAuthRequest("login", "email@test.com");
    AuthUser savedUser = AuthUser.builder().id(UUID.randomUUID()).login("login").build();

    when(authRepository.existsByLoginOrEmail("login", "email@test.com")).thenReturn(false);
    when(passwordEncoder.encode("SuperSecret123!")).thenReturn("hashed");
    when(authRepository.save(any(AuthUser.class))).thenReturn(savedUser);

    UUID result = authService.register(request);

    assertThat(result).isEqualTo(savedUser.getId());
    verify(authRepository).save(any(AuthUser.class));
    verify(authRepository, never()).delete(any());

    verify(authClient).sendUserData(any(UserDtoForUserService.class));
  }

  @Test
  void register_DuplicateLoginOrEmail_ThrowsCredentialsException() {
    when(authRepository.existsByLoginOrEmail(anyString(), anyString())).thenReturn(true);

    assertThatThrownBy(() -> authService.register(validAuthRequest("dup", "dup@test.com")))
        .isInstanceOf(CredentialsException.class);

    verify(authRepository, never()).save(any());
    verifyNoInteractions(authClient);
  }

  @Test
  void register_UserServiceThrowsException_ShouldThrowCredentialsException() {
    AuthRequest request = validAuthRequest("login", "email@test.com");
    AuthUser savedUser = AuthUser.builder().id(UUID.randomUUID()).login("login").build();

    when(authRepository.existsByLoginOrEmail(anyString(), anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    when(authRepository.save(any(AuthUser.class))).thenReturn(savedUser);

    doThrow(new CredentialsException(
        "Registration failed: User Service is unavailable. Connection refused"))
        .when(authClient).sendUserData(any(UserDtoForUserService.class));

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(CredentialsException.class)
        .hasMessageContaining("unavailable");
  }

  @Test
  void saveUserCredentials_Success_SavesEncodedPassword() {
    CredentialsRequest request = CredentialsRequest.builder()
        .login("cred_user")
        .password("SuperSecret123!")
        .email("cred@test.com")
        .role(Role.USER)
        .build();

    when(authRepository.existsByLoginOrEmail("cred_user", "cred@test.com")).thenReturn(false);
    when(passwordEncoder.encode("SuperSecret123!")).thenReturn("$2a$hashed");

    authService.saveUserCredentials(request);

    verify(authRepository).save(any(AuthUser.class));
    verifyNoInteractions(authClient);
  }

  @Test
  void saveUserCredentials_DuplicateLogin_ThrowsCredentialsException() {
    CredentialsRequest request = CredentialsRequest.builder()
        .login("existing")
        .password("SuperSecret123!")
        .email("existing@test.com")
        .role(Role.USER)
        .build();

    when(authRepository.existsByLoginOrEmail("existing", "existing@test.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.saveUserCredentials(request))
        .isInstanceOf(CredentialsException.class);

    verify(authRepository, never()).save(any());
  }

  @Test
  void login_Success_ReturnsTokenPair() {
    AuthUser user = AuthUser.builder()
        .id(UUID.randomUUID())
        .login("user")
        .password("hashed")
        .role(Role.USER)
        .build();

    when(authRepository.findByLogin("user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("SuperSecret123!", "hashed")).thenReturn(true);
    when(tokenService.generateToken(user.getId(), Role.USER, false)).thenReturn("access.token");
    when(tokenService.generateToken(user.getId(), Role.USER, true)).thenReturn("refresh.token");

    AuthResponse response = authService.login(new LoginRequest("user", "SuperSecret123!"));

    assertThat(response.getAccessToken()).isEqualTo("access.token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
  }

  @Test
  void login_WrongPassword_ThrowsCredentialsException() {
    AuthUser user = AuthUser.builder()
        .id(UUID.randomUUID()).login("user").password("hashed").build();

    when(authRepository.findByLogin("user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("WrongPass123!", "hashed")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("user", "WrongPass123!")))
        .isInstanceOf(CredentialsException.class)
        .hasMessage("Invalid credentials");
  }

  @Test
  void login_UnknownLogin_ThrowsCredentialsException_WithSameGenericMessage() {
    when(authRepository.findByLogin("nobody")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "SuperSecret123!")))
        .isInstanceOf(CredentialsException.class)
        .hasMessage("Invalid credentials");
  }

  @Test
  void login_AdminRole_TokenGeneratedWithAdminRole() {
    AuthUser admin = AuthUser.builder()
        .id(UUID.randomUUID()).login("admin").password("hashed").role(Role.ADMIN).build();

    when(authRepository.findByLogin("admin")).thenReturn(Optional.of(admin));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(tokenService.generateToken(admin.getId(), Role.ADMIN, false)).thenReturn("admin.access");
    when(tokenService.generateToken(admin.getId(), Role.ADMIN, true)).thenReturn("admin.refresh");

    AuthResponse response = authService.login(new LoginRequest("admin", "SuperSecret123!"));

    assertThat(response.getAccessToken()).isEqualTo("admin.access");
    verify(tokenService).generateToken(admin.getId(), Role.ADMIN, false);
    verify(tokenService).generateToken(admin.getId(), Role.ADMIN, true);
  }

  @Test
  void validateToken_DelegatesToTokenService() {
    Jwt mockJwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(UUID.randomUUID().toString())
        .claim("role", "USER")
        .build();

    when(tokenService.validateToken("token")).thenReturn(mockJwt);

    Jwt result = authService.validateToken("token");

    assertThat(result).isSameAs(mockJwt);
    verify(tokenService).validateToken("token");
  }

  @Test
  void refreshToken_DelegatesToTokenService() {
    AuthResponse expected = AuthResponse.builder()
        .accessToken("new.access").refreshToken("new.refresh").build();

    when(tokenService.refreshToken("refresh.token")).thenReturn(expected);

    AuthResponse result = authService.refreshToken("refresh.token");

    assertThat(result).isSameAs(expected);
    verify(tokenService).refreshToken("refresh.token");
  }

  private static AuthRequest validAuthRequest(String login, String email) {
    return AuthRequest.builder()
        .login(login)
        .password("SuperSecret123!")
        .email(email)
        .role(Role.USER)
        .name("Test")
        .surname("User")
        .birthDate(LocalDate.of(1995, 1, 1))
        .build();
  }

}