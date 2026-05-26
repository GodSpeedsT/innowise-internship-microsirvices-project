package com.innowise.authservice.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Component
public class SecurityFilters {

  @Bean
  @Order(1)
  public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {

    OAuth2AuthorizationServerConfigurer cfg = new OAuth2AuthorizationServerConfigurer();

    http.securityMatcher(cfg.getEndpointsMatcher())
        .with(cfg, Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain DEFAULTsECURITYfILTERcHAIN(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth -> auth.requestMatchers("/auth/**").permitAll().anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());
    return http.build();
  }

}
