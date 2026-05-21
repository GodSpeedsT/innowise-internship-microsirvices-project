package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>,
    JpaSpecificationExecutor<PaymentCard> {

  List<PaymentCard> findByUserId(UUID userId);

  Page<PaymentCard> findByUserId(UUID userId, Pageable pageable);

  @Query(value = "SELECT * FROM payment_cards WHERE holder ILIKE '%' || :holder || '%'", nativeQuery = true)
  List<PaymentCard> findCardsNative(@Param("holder") String holder);

  @Transactional
  @Modifying
  @Query("UPDATE PaymentCard pc SET pc.active = :active WHERE pc.id = :id")
  void setActiveStatus(@Param("id") UUID id, @Param("active") boolean active);


}
