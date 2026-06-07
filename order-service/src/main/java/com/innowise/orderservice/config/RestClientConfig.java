package com.innowise.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient restClient() {
    return RestClient.builder()
        .requestInterceptor(((request, body, execution) -> {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          if (auth != null && auth.getCredentials() instanceof Jwt jwt) {
            request.getHeaders().setBearerAuth(jwt.getTokenValue());
          }

          return execution.execute(request, body);
        }))
        .build();
  }

}
