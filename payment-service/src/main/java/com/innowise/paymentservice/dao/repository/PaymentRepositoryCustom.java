package com.innowise.paymentservice.dao.repository;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRepositoryCustom {

  List<Payment> findByFilters(UUID userId, UUID orderId, Status status, LocalDateTime from,
      LocalDateTime to);

  BigDecimal getTotalForUser(UUID userId, LocalDateTime from, LocalDateTime to);

  BigDecimal getTotalForAll();
}
