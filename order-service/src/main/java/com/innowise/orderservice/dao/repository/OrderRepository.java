package com.innowise.orderservice.dao.repository;

import com.innowise.orderservice.entity.Order;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>,
    JpaSpecificationExecutor<Order> {

  Page<Order> findByUserId(UUID userId, Pageable pageable);

}
