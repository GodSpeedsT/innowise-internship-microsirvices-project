package com.innowise.authservice.security;

import com.innowise.authservice.config.JwtKeyLoader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

  private final JwtKeyLoader jwtKeyLoader;

  @Bean
  public RSAPublicKey publicKey() {
    return jwtKeyLoader.getPublicKey();
  }

  @Bean
  public RSAPrivateKey privateKey() {
    return jwtKeyLoader.getPrivateKey();
  }

  @Bean
  public RSAKey rsaKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {

    String keyId = Integer.toHexString(publicKey.hashCode());
    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(keyId)
        .build();
  }

  @Bean
  JWKSet jwkSet(RSAKey rsaKey) {
    return new JWKSet(rsaKey);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
  }
}
