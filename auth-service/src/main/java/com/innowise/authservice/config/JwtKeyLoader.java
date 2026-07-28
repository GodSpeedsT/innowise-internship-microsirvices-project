package com.innowise.authservice.config;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Getter
@Slf4j
@Component
public class JwtKeyLoader {

  @Value("${jwt.private-key-location:classpath:keys/private.pem}")
  private Resource privateKeyLocation;
  @Value("${jwt.public-key-location:classpath:keys/public.pem}")
  private Resource publicKeyLocation;

  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;

  @PostConstruct
  public void init() throws Exception {
    try {
      this.publicKey = loadPublicKey();
      this.privateKey = loadPrivateKey();
      log.info("Public Key: {}", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
    } catch (Exception e) {
      log.error("Unable to load keys, generating. Reason: ", e);
      KeyPair pair = generateKeyPair();
      this.privateKey = (RSAPrivateKey) pair.getPrivate();
      this.publicKey = (RSAPublicKey) pair.getPublic();
    }
  }

  private RSAPublicKey loadPublicKey() throws Exception {
    try (InputStream in = publicKeyLocation.getInputStream()) {
      String key = new String(in.readAllBytes())
          .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(key);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
    }
  }

  private RSAPrivateKey loadPrivateKey() throws Exception {
   try (InputStream in = privateKeyLocation.getInputStream()) {
      String key = new String(in.readAllBytes())
          .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(key);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }
  }

  private KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    return gen.generateKeyPair();
  }

}
