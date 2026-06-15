package com.innowise.orderservice.dao.repository;

import com.innowise.orderservice.entity.Item;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

  Optional<Item> findByName(String name);
}
