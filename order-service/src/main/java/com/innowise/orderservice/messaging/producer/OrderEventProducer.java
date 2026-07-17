package com.innowise.orderservice.messaging.producer;

import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.messaging.event.OrderCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

  private static final String TOPIC = "order-events";

  private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;
  private final OrderRepository orderRepository;

  public void sendOrderCompletedEvent(UUID orderId) {

    OrderCompletedEvent event = OrderCompletedEvent.builder()
        .id(UUID.randomUUID())
        .orderId(orderId)
        .billAmount(getTotalPrice(orderId))
        .build();

    kafkaTemplate.send(TOPIC, event.getId().toString(), event);
  }

  private BigDecimal getTotalPrice(UUID orderId) {
    return orderRepository.findById(orderId)
        .map(Order::getTotalPrice)
        .orElseThrow(() -> new EntityNotFoundException("Order not found with id", orderId));
  }

}
