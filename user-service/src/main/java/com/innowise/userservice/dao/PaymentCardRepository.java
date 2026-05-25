package com.innowise.userservice.dao;

import com.innowise.userservice.entity.PaymentCard;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>,
    JpaSpecificationExecutor<PaymentCard> {

  List<PaymentCard> findByUserId(UUID userId);

  Optional<PaymentCard> findByNumber(String cardNumber);

  @Query(value = "SELECT * FROM payment_cards WHERE user_id = :userId", nativeQuery = true)
  List<PaymentCard> findCardsByUserIdNative(@Param("userId") UUID userId);

  @Transactional
  @Modifying
  @Query("UPDATE PaymentCard pc SET pc.active = :active WHERE pc.id = :id")
  void setActiveStatus(@Param("id") UUID id, @Param("active") boolean active);


}
