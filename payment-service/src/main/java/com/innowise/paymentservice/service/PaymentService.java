package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

  PaymentResponse createPayment(PaymentRequest request);

  List<PaymentResponse> getAllPayments(UUID userId, UUID orderId, Status status,
      LocalDateTime from, LocalDateTime to);

  PaymentResponse getPaymentById(String paymentId);

  BigDecimal getTotalForUser(UUID userId, LocalDateTime from, LocalDateTime to);

  BigDecimal getTotalForAll();

}
