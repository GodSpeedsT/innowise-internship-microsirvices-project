package com.innowise.paymentservice.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.integration.BaseIntegrationTest;
import com.innowise.paymentservice.messaging.producer.PaymentEventProducer;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

class PaymentControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private PaymentRepository paymentRepository;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private JwtDecoder jwtDecoder;
  @MockitoBean
  private PaymentEventProducer producer;
  @Autowired
  private PaymentEventProducer paymentEventProducer;

  String paymentId;

  @BeforeEach
  void setUp() {
    paymentId = UUID.randomUUID().toString();
    paymentRepository.deleteAll();
  }

  @Test
  @WithMockUser(username = userId, roles = "USER")
  @DisplayName("Create payment success")
  void createPayment_success() throws Exception {

    paymentRepository.save(Payment.builder()
        .orderId(orderId)
        .status(Status.PENDING)
        .build());

    stubExternalApi();

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PaymentRequest(orderId))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    Payment saved = paymentRepository.findByOrderId(orderId).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(Status.SUCCESS);

    verify(paymentEventProducer).sendPaymentCompleted(orderId, Status.SUCCESS);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getPaymentById_success() throws Exception {

    paymentRepository.save(Payment.builder()
        .id(paymentId)
        .orderId(orderId)
        .userId(UUID.fromString(userId))
        .paymentAmount(BigDecimal.valueOf(10000))
        .status(Status.SUCCESS)
        .build());

    mockMvc.perform(get("/api/v1/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.paymentAmount").value(BigDecimal.valueOf(10000)));

    assertThat(paymentRepository.findById(paymentId)).isPresent();
    assertEquals(paymentId, paymentRepository.findById(paymentId).get().getId());

  }


  @Test
  @DisplayName("GetAllPayments - success")
  @WithMockUser(username = userId, roles = "ADMIN")
  void getAllPayments_success() throws Exception {
    List<Payment> payments = List.of(Payment.builder()
            .id(paymentId)
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(30000))
            .status(Status.SUCCESS)
            .build(),
        Payment.builder()
            .id(UUID.randomUUID().toString())
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(50000))
            .status(Status.FAILED)
            .build());
    paymentRepository.saveAll(payments);

    mockMvc.perform(get("/api/v1/payments"))
        .andExpect(status().isOk());

    assertThat(paymentRepository.findAll()).hasSize(2);
    assertThat(paymentRepository.findById(paymentId)).isPresent();

  }

  @Test
  @WithMockUser(username = userId, roles = "USER")
  @DisplayName("getUserSuccessPaymentsSummary - success")
  void getUserSuccessPaymentsSummary_success() throws Exception {
    List<Payment> payments = List.of(Payment.builder()
            .id(paymentId)
            .userId(UUID.fromString(userId))
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(30000))
            .status(Status.SUCCESS)
            .build(),
        Payment.builder()
            .id(UUID.randomUUID().toString())
            .userId(UUID.fromString(userId))
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(50000))
            .status(Status.SUCCESS)
            .build());
    paymentRepository.saveAll(payments);

    MvcResult result = mockMvc.perform(get("/api/v1/payments/users/" + userId + "/summary"))
        .andExpect(status().isOk())
        .andReturn();

    BigDecimal summary = new BigDecimal(result.getResponse().getContentAsString());

    assertThat(paymentRepository.findAll()).hasSize(2);
    assertThat(paymentRepository.findById(paymentId)).isPresent();
    assertThat(summary).isEqualByComparingTo(BigDecimal.valueOf(80000));
  }

  @Test
  @WithMockUser(username = userId, roles = "ADMIN")
  @DisplayName("getAllSuccessPaymentsSummary - success")
  void getAllSuccessPaymentsSummary_success() throws Exception {
    List<Payment> payments = List.of(Payment.builder()
            .id(paymentId)
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(30000))
            .status(Status.SUCCESS)
            .build(),
        Payment.builder()
            .id(UUID.randomUUID().toString())
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .paymentAmount(BigDecimal.valueOf(50000))
            .status(Status.SUCCESS)
            .build());
    paymentRepository.saveAll(payments);

    MvcResult result = mockMvc.perform(get("/api/v1/payments/summary"))
        .andExpect(status().isOk())
        .andReturn();

    BigDecimal answer = new BigDecimal(result.getResponse().getContentAsString());

    assertThat(paymentRepository.findAll()).hasSize(2);
    assertThat(paymentRepository.findById(paymentId)).isPresent();
    assertThat(answer).isEqualByComparingTo(BigDecimal.valueOf(80000));
  }

}
