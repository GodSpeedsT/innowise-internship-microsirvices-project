package com.innowise.userservice.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CustomIntrospector implements OpaqueTokenIntrospector {

  private final RestTemplate restTemplate = new RestTemplate();
  private static final String ROLE_PREFIX = "ROLE_";
  @Value("${auth-service.url}")
  private String validateUrl;

  @Override
  public OAuth2AuthenticatedPrincipal introspect(String token) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Content-Type", "application/x-www-form-urlencoded");

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("token", token);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

      log.info("Sending token to auth-service for validation ");
      Map<String, Object> response = restTemplate.postForObject(validateUrl, request, Map.class);

      log.info("Response from auth-service: {}", response);
      if (response != null && Boolean.TRUE.equals(response.get("valid")) || "true".equals(
          String.valueOf(Objects.requireNonNull(response).get("valid")))) {
        String userId = String.valueOf(response.get("userId"));
        String role = String.valueOf(response.get("role"));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(ROLE_PREFIX + role);

        Map<String, Object> attributes = Map.of(
            "sub", userId,
            "userId", userId,
            "role", role,
            "scope", List.of(ROLE_PREFIX + role),
            "authorities", List.of(ROLE_PREFIX + role)
        );

        log.info("Successfully authenticated user {} with authority ROLE_{}", userId, role);
        return new OAuth2IntrospectionAuthenticatedPrincipal(userId, attributes,
            List.of(authority));
      }
      log.warn("Auth-service returned valid=false or empty response");
    } catch (Exception e) {
      log.error("Error during token introspection: ", e);
      throw new OAuth2AuthenticationException("Token validation failed via auth-service");
    }
    throw new OAuth2AuthenticationException("Invalid token");
  }
}
