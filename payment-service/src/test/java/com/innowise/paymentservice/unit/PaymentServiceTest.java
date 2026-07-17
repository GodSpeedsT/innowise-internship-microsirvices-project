package com.innowise.paymentservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.ResourceNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.messaging.producer.PaymentEventProducer;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;
  @Mock
  private PaymentEventProducer producer;
  @Mock
  private PaymentMapper paymentMapper;
  @Mock
  private Authentication authentication;
  @Mock
  private SecurityContext securityContext;

  @InjectMocks
  @Spy
  private PaymentServiceImpl paymentService;

  private final UUID mockUserId = UUID.randomUUID();
  private final UUID mockOrderId = UUID.randomUUID();
  private final String mockPaymentId = UUID.randomUUID().toString();

  @BeforeEach
  void setUpSecurityContext() {

    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithMockUser("USER")
  @DisplayName("createPayment – success: save payment with data from order-service")
  void createPayment_success() {
    PaymentRequest request = new PaymentRequest(mockOrderId);
    Payment mockPayment = Payment.builder()
        .id(mockPaymentId)
        .orderId(mockOrderId)
        .userId(mockUserId)
        .status(Status.PENDING)
        .paymentAmount(BigDecimal.valueOf(15000.00))
        .timestamp(LocalDateTime.now())
        .build();

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(mockUserId.toString());
    when(paymentRepository.findByOrderId(mockOrderId)).thenReturn(Optional.of(mockPayment));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    PaymentResponse mockResponse = new PaymentResponse();
    mockResponse.setStatus(Status.SUCCESS);
    when(paymentMapper.toResponse(any(Payment.class))).thenReturn(mockResponse);

    doReturn(Status.SUCCESS).when(paymentService).defineStatus();

    PaymentResponse response = paymentService.createPayment(request);

    assertNotNull(response);
    assertEquals(Status.SUCCESS, response.getStatus());

    assertEquals(mockUserId, mockPayment.getUserId());
    assertEquals(Status.SUCCESS, mockPayment.getStatus());

    verify(paymentRepository, times(1)).findByOrderId(mockOrderId);
    verify(paymentRepository, times(1)).save(mockPayment);
    verify(producer, times(1)).sendPaymentCompleted(mockOrderId, Status.SUCCESS);
  }

  @Test
  @DisplayName("createPayment – returns ResourceNotFoundException")
  void createPayment_returns_exception() {

    when(paymentRepository.findByOrderId(mockOrderId)).thenReturn(Optional.empty());

    PaymentRequest request = new PaymentRequest(mockOrderId);

    assertThrows(ResourceNotFoundException.class,
        () -> paymentService.createPayment(request));

  }

  @Test
  @WithMockUser("ADMIN")
  @DisplayName("getAllPayments – success: print all payments for admin")
  void getAllPayments_success() {
    Payment mockPayment = Payment.builder()
        .orderId(mockOrderId)
        .paymentAmount(BigDecimal.valueOf(15000.00))
        .status(Status.SUCCESS)
        .userId(mockUserId)
        .build();

    when(paymentRepository.findByFilters(eq(mockUserId), eq(mockOrderId), eq(Status.SUCCESS), any(),
        any()))
        .thenReturn(List.of(mockPayment));

    PaymentResponse mockResponseDto = new PaymentResponse();
    mockResponseDto.setUserId(mockUserId);
    mockResponseDto.setStatus(Status.SUCCESS);

    when(paymentMapper.toResponse(mockPayment)).thenReturn(mockResponseDto);

    LocalDateTime now = LocalDateTime.now();
    List<PaymentResponse> responses = paymentService.getAllPayments(mockUserId, mockOrderId,
        Status.SUCCESS, now, now);

    assertNotNull(responses);
    assertEquals(1, responses.size());
    assertEquals(mockUserId, responses.getFirst().getUserId());
    assertEquals(Status.SUCCESS, responses.getFirst().getStatus());
  }

  @Test
  @WithMockUser("ADMIN")
  @DisplayName("getPaymentById – success: print payment for admin")
  void getPaymentById_success() {
    Payment mockPayment = Payment.builder()
        .id(mockPaymentId)
        .orderId(mockOrderId)
        .paymentAmount(BigDecimal.valueOf(15000.00))
        .status(Status.SUCCESS)
        .userId(mockUserId)
        .build();

    PaymentResponse response = new PaymentResponse();
    response.setId(mockPaymentId);
    response.setStatus(Status.SUCCESS);
    response.setOrderId(mockOrderId);
    response.setUserId(mockUserId);
    response.setPaymentAmount(BigDecimal.valueOf(15000.00));

    when(paymentRepository.findById(mockPaymentId)).thenReturn(Optional.of(mockPayment));
    when(paymentMapper.toResponse(mockPayment)).thenReturn(response);

    PaymentResponse resp = paymentService.getPaymentById(mockPaymentId);

    assertNotNull(resp);
    assertEquals(mockPaymentId, resp.getId());
    assertEquals(mockUserId, resp.getUserId());
    assertEquals(Status.SUCCESS, resp.getStatus());
  }

  @Test
  @WithMockUser("ADMIN")
  @DisplayName("getPaymentById – returns exception")
  void getPaymentById_returns_exception() {
    String nonExistingId = "7bjbjvbjffbjnwnkn";

    when(paymentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () ->
        paymentService.getPaymentById(nonExistingId));
  }

  @Test
  @WithMockUser("USER")
  @DisplayName("getTotalForUser - success, return total sum of user`s payments")
  void getTotalForUser_success() {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime to = LocalDateTime.now();

    when(paymentRepository.getTotalForUser(mockUserId, now, to)).thenReturn(
        BigDecimal.valueOf(35000.00));

    BigDecimal answer = paymentService.getTotalForUser(mockUserId, now,
        to);
    assertNotNull(answer);
    assertEquals(BigDecimal.valueOf(35000.00), answer);
    verify(paymentRepository, times(1)).getTotalForUser(mockUserId, now, to);
  }

  @Test
  @WithMockUser("ADMIN")
  @DisplayName("getTotalForAll - success, return total sum of all users payments")
  void getTotalForAll_success() {

    when(paymentRepository.getTotalForAll()).thenReturn(BigDecimal.valueOf(70000.00));

    BigDecimal answer = paymentService.getTotalForAll();
    assertNotNull(answer);
    assertEquals(BigDecimal.valueOf(70000.00), answer);
    verify(paymentRepository, times(1)).getTotalForAll();
  }

}




