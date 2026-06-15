package com.innowise.orderservice.dao.specifications;

import com.innowise.orderservice.entity.Item;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecification {

  public static Specification<Item> filterByName(String name) {
    return (root, query, cb) -> {
      if (name == null) {
        return cb.conjunction();
      }
      return cb.like(root.get("name"), "%" + name + "%");
    };
  }

  public static Specification<Item> filterByPrice(BigDecimal price) {
    return ((root, query, criteriaBuilder) -> {
      if (price == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("price"), price);
    });
  }

  private ItemSpecification() {
  }

}
