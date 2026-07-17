package com.innowise.paymentservice.messaging.consumer;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.messaging.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

  private final PaymentRepository paymentRepository;

  @KafkaListener(topics = "order-events", groupId = "payment-service")
  public void handleOrderEvent(OrderCompletedEvent event) {
    log.info("Received order event: orderId = {}, income = {}", event.getOrderId(),
        event.getBillAmount());

    paymentRepository.findByOrderId(event.getOrderId()).ifPresentOrElse(payment -> {
          payment.setPaymentAmount(event.getBillAmount());
          paymentRepository.save(payment);
        },
        () -> {
          Payment payment = new Payment();
          payment.setOrderId(event.getOrderId());
          payment.setPaymentAmount(event.getBillAmount());
          payment.setStatus(Status.PENDING);
          paymentRepository.save(payment);
        });
  }

}
