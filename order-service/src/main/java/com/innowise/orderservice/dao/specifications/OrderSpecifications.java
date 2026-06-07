package com.innowise.orderservice.dao.specifications;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecifications {

  public static Specification<Order> filterByCreationDate(LocalDateTime start, LocalDateTime end) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();

      if (start != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
      }
      if (end != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
  }

  public static Specification<Order> filterByStatus(String status) {
    return (root, query, cb) -> {
      if (status == null || status.isBlank()) {
        return cb.conjunction();
      }
      try {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toLowerCase());
        return cb.equal(root.get("status"), orderStatus);
      } catch (IllegalArgumentException e) {
        return cb.disjunction();
      }
    };
  }

  private OrderSpecifications() {
  }

}
