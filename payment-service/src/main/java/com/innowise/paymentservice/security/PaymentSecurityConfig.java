package com.innowise.paymentservice.security;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@RequiredArgsConstructor
public class PaymentSecurityConfig {

  private final PaymentRepository paymentRepository;

  public boolean isOwner(String paymentId, Authentication authentication) {
    if (authentication.getName() == null) {
      return false;
    }
    return paymentRepository.findById(paymentId)
        .map(payment -> payment.getUserId().toString().equals(authentication.getName()))
        .orElse(false);
  }

  public boolean isAdmin(Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ADMIN"::equals);
  }

  public UUID resolveUserId(UUID requestedUserId, Authentication authentication) {
    if (isAdmin(authentication)) {
      return requestedUserId;
    }
    return currentUserId(authentication);
  }

  public UUID currentUserId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

}
