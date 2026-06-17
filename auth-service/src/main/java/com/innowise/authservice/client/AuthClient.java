package com.innowise.authservice.client;

import com.innowise.authservice.dto.UserDtoForUserService;
import com.innowise.authservice.exception.CredentialsException;
import com.innowise.authservice.repository.AuthRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

  @Value("${user-service.url}")
  private String userServiceUrl;

  private final RestClient restClient;
  private final AuthRepository authRepository;

  public void sendUserData(UserDtoForUserService dto) {
    try {
      restClient.post()
          .uri(userServiceUrl + "/api/v1/users")
          .contentType(MediaType.APPLICATION_JSON)
          .body(dto)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("User-service call failed during registration for id={}; rolling back",
          dto.getId(), e);
      throw new CredentialsException(
          "Registration failed: User Service is unavailable. " + e.getMessage());
    }
  }

}
