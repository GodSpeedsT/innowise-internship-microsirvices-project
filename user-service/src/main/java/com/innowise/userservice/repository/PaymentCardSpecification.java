package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {

  public static Specification<PaymentCard> filterHolder(String holder) {
    return ((root, query, criteriaBuilder) -> {
      if (holder == null || holder.isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("holder")),
          "%" + holder.toLowerCase() + "%");
    });
  }

  public static Specification<PaymentCard> filterByActive(Boolean active) {
    return (root, query, cb) -> {
      if (active == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("active"), active);
    };
  }

}
