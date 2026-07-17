package com.innowise.paymentservice.dao.repository;

import com.innowise.paymentservice.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String>,
    PaymentRepositoryCustom {

  Optional<Payment> findByOrderId(UUID orderId);

}
