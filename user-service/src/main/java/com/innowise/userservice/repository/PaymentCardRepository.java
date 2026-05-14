package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID> {
PaymentCard save(PaymentCard paymentCard);
Optional<PaymentCard> findById(UUID id);

}
