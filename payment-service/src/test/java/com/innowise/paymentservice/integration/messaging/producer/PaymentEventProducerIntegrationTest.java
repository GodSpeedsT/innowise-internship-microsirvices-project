package com.innowise.paymentservice.integration.messaging.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.integration.BaseIntegrationTest;
import com.innowise.paymentservice.messaging.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

class PaymentEventProducerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private PaymentRepository paymentRepository;
  @MockitoBean
  private JwtDecoder jwtDecoder;

  @Test
  @WithMockUser(username = userId, roles = "USER")
  @DisplayName("createPayment publishes PaymentCompletedEvent to payment-events")
  void createPayment_publishesPaymentCompletedEvent() throws Exception {

    paymentRepository.save(Payment.builder()
        .id(UUID.randomUUID().toString())
        .userId(UUID.fromString(userId))
        .orderId(orderId)
        .paymentAmount(BigDecimal.valueOf(15000))
        .build());

    stubExternalApi();

    kafkaConsumer.subscribe(List.of("payment-events"));
    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new PaymentRequest(orderId))))
        .andExpect(status().isAccepted());

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      ConsumerRecords<String, PaymentCompletedEvent> records = kafkaConsumer.poll(
          Duration.ofSeconds(10));
      assertThat(records).isNotEmpty();
      assertThat(records.iterator().next().value().getOrderId()).isEqualTo(orderId);
    });

  }
}
