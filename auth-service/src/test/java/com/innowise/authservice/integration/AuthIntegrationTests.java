package com.innowise.authservice.integration;

import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.CredentialsRequest;
import com.innowise.authservice.dto.TokenRequest;
import com.innowise.authservice.entity.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.repository.AuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTests {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private AuthRepository authRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @MockitoBean
  private RestClient restClientBuilder;

  @Container
  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      "postgres:latest")
      .withDatabaseName("secret")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    authRepository.deleteAll();
  }

  @Test
  void register_UserServiceFails_ShouldRollbackCredentials() throws Exception {

    mockMvc.perform(post("/api/v1/auth/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validAuthRequest("fail_user", "fail@test.com"))))
        .andExpect(status().isUnauthorized());

    assertThat(authRepository.findByLogin("fail_user")).isEmpty();
  }

  @Test
  void register_DuplicateLogin_Returns401() throws Exception {
    authRepository.save(AuthUser.builder()
        .login("dup_user")
        .email("dup@test.com")
        .password(passwordEncoder.encode("SuperSecret123!"))
        .role(Role.USER)
        .build());

    mockMvc.perform(post("/api/v1/auth/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validAuthRequest("dup_user", "dup@test.com"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void saveCredentials_Success_Returns201() throws Exception {
    mockMvc.perform(post("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validCredentials("cred_user", "cred@test.com"))))
        .andExpect(status().isCreated());

    assertThat(authRepository.findByLogin("cred_user")).isPresent();
  }

  @Test
  void saveCredentials_DuplicateEmail_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(validCredentials("first_user", "shared@test.com"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(validCredentials("second_user", "shared@test.com"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_WithCorrectCredentials_ShouldReturnJwtTokens() throws Exception {
    mockMvc.perform(post("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validCredentials("login_test", "login@test.com"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(new LoginRequest("login_test", "SuperSecret123!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  void login_WrongPassword_Returns401WithGenericMessage() throws Exception {
    saveCredentialsSilently("wrongpw_user", "wrongpw@test.com");

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(new LoginRequest("wrongpw_user", "WrongPassword999!"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void login_UnknownLogin_Returns401WithSameGenericMessage() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest("nobody", "SuperSecret123!"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void validate_WithValidAccessToken_ReturnsUserIdAndRole() throws Exception {
    saveCredentialsSilently("val_user", "val@test.com");
    String accessToken = loginAndGetTokens("val_user").getAccessToken();

    mockMvc.perform(post("/api/v1/auth/tokens/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest(accessToken))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void validate_AdminRole_ClaimPresentInToken() throws Exception {
    mockMvc.perform(post("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                validCredentials("admin_user", "admin@test.com", Role.ADMIN))))
        .andExpect(status().isCreated());

    String accessToken = loginAndGetTokens("admin_user").getAccessToken();

    mockMvc.perform(post("/api/v1/auth/tokens/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest(accessToken))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void validate_BogusToken_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/auth/tokens/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest("not.a.jwt"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_WithValidRefreshToken_ReturnsNewTokenPair() throws Exception {
    saveCredentialsSilently("ref_user", "ref@test.com");
    String refreshToken = loginAndGetTokens("ref_user").getRefreshToken();

    MvcResult result = mockMvc.perform(post("/api/v1/auth/tokens/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest(refreshToken))))
        .andExpect(status().isOk())
        .andReturn();

    AuthResponse newPair = objectMapper.readValue(
        result.getResponse().getContentAsString(), AuthResponse.class);

    assertThat(newPair.getAccessToken()).isNotBlank();
    assertThat(newPair.getRefreshToken()).isNotBlank();
  }

  @Test
  void refresh_WithAccessTokenInsteadOfRefresh_Returns401() throws Exception {
    saveCredentialsSilently("ref_wrong", "refwrong@test.com");
    String accessToken = loginAndGetTokens("ref_wrong").getAccessToken();

    mockMvc.perform(post("/api/v1/auth/tokens/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest(accessToken))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_BogusToken_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/auth/tokens/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TokenRequest("garbage.token.value"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void password_StoredAsBcryptHash_NotPlaintext() {
    saveCredentialsSilently("bcrypt_user", "bcrypt@test.com");

    String stored = authRepository.findByLogin("bcrypt_user")
        .orElseThrow().getPassword();

    assertThat(stored).startsWith("$2a$");
    assertThat(stored).isNotEqualTo("SuperSecret123!");
  }

  private AuthResponse loginAndGetTokens(String login) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(login, "SuperSecret123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
  }

  private void saveCredentialsSilently(String login, String email) {
    try {
      mockMvc.perform(post("/api/v1/auth/credentials")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validCredentials(login, email))))
          .andExpect(status().isCreated());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  private static CredentialsRequest validCredentials(String login, String email) {
    return validCredentials(login, email, Role.USER);
  }

  private static CredentialsRequest validCredentials(String login, String email, Role role) {
    return CredentialsRequest.builder()
        .login(login)
        .password("SuperSecret123!")
        .email(email)
        .role(role)
        .build();
  }

}