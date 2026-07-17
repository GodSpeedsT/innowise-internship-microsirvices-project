package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.request.PaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.security.PaymentSecurityConfig;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final PaymentSecurityConfig paymentSecurityConfig;

  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  @PostMapping
  public ResponseEntity<PaymentResponse> createPayment(
      @Valid @RequestBody PaymentRequest paymentRequest) {
    return ResponseEntity.accepted().body(paymentService.createPayment(paymentRequest));
  }

  @PreAuthorize("hasRole('ADMIN') OR @paymentSecurityConfig.isOwner(#id,authentication)")
  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
    return ResponseEntity.ok().body(paymentService.getPaymentById(id));
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @GetMapping
  public ResponseEntity<List<PaymentResponse>> getAllPayments(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) UUID orderId,
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      Authentication authentication
  ) {
    UUID effectiveUserId = paymentSecurityConfig.resolveUserId(userId, authentication);
    return ResponseEntity.ok(
        paymentService.getAllPayments(effectiveUserId, orderId, status, from, to));
  }

  @PreAuthorize("hasRole('ADMIN') OR #userId.toString() == authentication.name")
  @GetMapping("/users/{userId}/summary")
  public ResponseEntity<BigDecimal> getUserSuccessPaymentsSummary(
      @PathVariable UUID userId,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to
  ) {
    return ResponseEntity.ok()
        .body(paymentService.getTotalForUser(userId, from, to));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/summary")
  public ResponseEntity<BigDecimal> getAllSuccessPaymentsSummary() {
    return ResponseEntity.ok()
        .body(paymentService.getTotalForAll());
  }

}
