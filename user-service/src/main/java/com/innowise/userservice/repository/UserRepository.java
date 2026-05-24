package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  boolean existsByEmail(String email);

  @Modifying
  @Transactional
  @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
  void setActiveStatus(@Param("id") UUID id, @Param("active") boolean active);

  @Query("SELECT COUNT(pc) FROM PaymentCard pc WHERE pc.user.id = :userId")
  long countCardsByUserId(@Param("userId") UUID userId);

}
