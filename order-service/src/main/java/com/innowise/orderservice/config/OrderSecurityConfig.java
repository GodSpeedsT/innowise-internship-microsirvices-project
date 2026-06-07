package com.innowise.orderservice.config;

import com.innowise.orderservice.dao.repository.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

@Configuration("orderSecurityConfig")
@RequiredArgsConstructor
public class OrderSecurityConfig {

  private final OrderRepository orderRepository;

  public boolean isOwner(UUID orderId, Authentication authentication) {
    return orderRepository.findById(orderId)
        .map(order -> order.getUserId().toString().equals(authentication.getName()))
        .orElse(false);
  }

}
