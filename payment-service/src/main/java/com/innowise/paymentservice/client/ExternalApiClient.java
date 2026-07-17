package com.innowise.paymentservice.client;

import com.innowise.paymentservice.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExternalApiClient {

  private final RestClient restClient;

  @Value("${api.external.url}")
  private String baseUrl;

  public Long getRandomNumber() {
    try {
      String responseBody = restClient.get()
          .uri(baseUrl)
          .retrieve()
          .body(String.class);
      if (responseBody == null || responseBody.isEmpty()) {
        throw new ExternalServiceException("External service returned empty response body");
      }
      return Long.parseLong(responseBody.trim());
    } catch (Exception e) {
      log.error("External service is unavailable: {}", e.getMessage(), e);
      throw new ExternalServiceException("External service unavailable: " + e.getMessage());
    }
  }

}
