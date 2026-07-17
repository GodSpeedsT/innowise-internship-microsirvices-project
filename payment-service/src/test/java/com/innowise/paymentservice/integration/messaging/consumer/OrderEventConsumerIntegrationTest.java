package com.innowise.paymentservice.integration.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.integration.BaseIntegrationTest;
import com.innowise.paymentservice.messaging.event.OrderCompletedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderEventConsumerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private PaymentRepository paymentRepository;

  @Test
  @DisplayName("Test kafka consumer")
  void testKafkaConsumer() {
    OrderCompletedEvent orderCompletedEvent = OrderCompletedEvent.builder()
        .id(UUID.randomUUID())
        .orderId(orderId)
        .billAmount(BigDecimal.valueOf(30000))
        .build();

    kafkaProducer.send(new ProducerRecord<>("order-events", orderCompletedEvent));

    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          Optional<Payment> saved = paymentRepository.findByOrderId(
              orderCompletedEvent.getOrderId());
          assertThat(saved).isPresent();
          assertThat(saved.get().getStatus()).isEqualTo(Status.PENDING);
          assertThat(saved.get().getPaymentAmount()).isEqualByComparingTo(
              BigDecimal.valueOf(30000));
        });
  }

}
