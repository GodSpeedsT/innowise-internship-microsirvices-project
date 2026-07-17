package com.innowise.paymentservice.messaging.producer;

import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.messaging.event.PaymentCompletedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

  private static final String TOPIC = "payment-events";

  private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

  public void sendPaymentCompleted(UUID orderId, Status status) {
    PaymentCompletedEvent event = PaymentCompletedEvent
        .builder()
        .orderId(orderId)
        .status(status.name())
        .build();

    kafkaTemplate.send(TOPIC, orderId.toString(), event);
  }

}
