package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.dao.repository.PaymentRepository;
import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.ResourceNotFoundException;
import com.innowise.paymentservice.messaging.producer.PaymentEventProducer;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final ExternalApiClient externalApiClient;
  private final PaymentEventProducer producer;

  @Transactional
  public PaymentResponse createPayment(PaymentRequest request) {
    Payment payment = paymentRepository.findByOrderId(request.getOrderId()).orElseThrow(
        () -> new ResourceNotFoundException(
            "Payment not found for orderId: " + request.getOrderId()));

    UUID userId = UUID.fromString(
        Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName());
    payment.setUserId(userId);
    payment.setStatus(defineStatus());
    paymentRepository.save(payment);

    producer.sendPaymentCompleted(payment.getOrderId(), payment.getStatus());
    return paymentMapper.toResponse(payment);
  }

  public List<PaymentResponse> getAllPayments(UUID userId, UUID orderId, Status status,
      LocalDateTime from, LocalDateTime to) {
    return paymentRepository.findByFilters(userId, orderId, status, from, to)
        .stream()
        .map(paymentMapper::toResponse)
        .toList();
  }

  public PaymentResponse getPaymentById(String paymentId) {
    return paymentMapper.toResponse(findPaymentOrThrow(paymentId));
  }

  public BigDecimal getTotalForUser(UUID userId, LocalDateTime from, LocalDateTime to) {
    return paymentRepository.getTotalForUser(userId, from, to);
  }

  public BigDecimal getTotalForAll() {
    return paymentRepository.getTotalForAll();
  }

  private Payment findPaymentOrThrow(String paymentId) {
    return paymentRepository.findById(paymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
  }

  public Status defineStatus() {
    if (externalApiClient.getRandomNumber() % 2 == 0) {
      return Status.SUCCESS;
    }
    return Status.FAILED;
  }

}
