package com.innowise.orderservice.config;

import com.innowise.orderservice.dto.response.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

  private final RestClient restClient;

  @Value("${user-service.url}")
  private String userServiceUrl;

  @CircuitBreaker(name = "userService",fallbackMethod = "getUserInfoFallback")
  public UserResponse getUserInfo(UUID userId) {
    return restClient.get()
        .uri(userServiceUrl + userId)
        .retrieve()
        .body(UserResponse.class);
  }

  public UserResponse getUserInfoFallback(UUID userId, Throwable throwable) {
    log.warn("Circuit breaker triggered for user-service, userId = {} : {}", userId,
        throwable.getMessage());
    return null;
  }

}
