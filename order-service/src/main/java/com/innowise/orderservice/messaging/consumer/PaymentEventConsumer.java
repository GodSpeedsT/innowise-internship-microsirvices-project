package com.innowise.orderservice.messaging.consumer;

import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.messaging.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

  private final OrderRepository orderRepository;

  @KafkaListener(topics = "payment-events", groupId = "order-service")
  public void handlePaymentCompleted(PaymentCompletedEvent event) {
    log.info("Payment event received: orderId={}, status={}", event.getOrderId(),
        event.getStatus());

    orderRepository.findById(event.getOrderId()).ifPresent(order -> {
      if ("SUCCESS".equals(event.getStatus())) {
        order.setStatus(OrderStatus.PAID);
      } else {
        order.setStatus(OrderStatus.UNPAID);
      }
      orderRepository.save(order);
    });
  }

}
