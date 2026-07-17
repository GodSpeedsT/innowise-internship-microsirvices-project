package com.innowise.orderservice.integration.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.awaitility.Awaitility.await;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.integration.BaseIntegrationTest;
import com.innowise.orderservice.messaging.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaymentConsumerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private ItemRepository itemRepository;

  @Test
  @DisplayName("Test kafka consumer")
  void testKafkaConsumer() {
    Item item = itemRepository.save(Item.builder()
        .name("test item")
        .price(BigDecimal.valueOf(10000))
        .build());

    Order order = Order.builder()
        .userId(UUID.randomUUID())
        .status(OrderStatus.PAID)
        .totalPrice(BigDecimal.valueOf(10000))
        .build();

    List<OrderItem> items = List.of(OrderItem.builder()
        .order(order)
        .item(item)
        .quantity(3L)
        .build());

    order.setOrderItems(items);
    Order savedOrder = orderRepository.save(order);

    PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEvent.builder()
        .orderId(savedOrder.getId())
        .status("SUCCESS")
        .build();

    kafkaProducer.send(new ProducerRecord<>("payment-events", paymentCompletedEvent));

    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          Optional<Order> saved = orderRepository.findById(paymentCompletedEvent.getOrderId());
          assertThat(saved).isPresent();
          assertThat(saved.get().getStatus()).isEqualTo(OrderStatus.PAID);
        });
  }

}

