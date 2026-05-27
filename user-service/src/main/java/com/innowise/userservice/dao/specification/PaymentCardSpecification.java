package com.innowise.userservice.dao.specification;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {

  public static Specification<PaymentCard> hasUserFirstAndSurname(String name, String surname) {
    return (root, query, cb) -> {
      if (name == null && surname == null) {
        return cb.conjunction();
      }

      var userJoin = root.join("user");

      if (name != null && surname != null) {
        return cb.and(
            cb.like(cb.lower(userJoin.get("name")), "%" + name.toLowerCase() + "%"),
            cb.like(cb.lower(userJoin.get("surname")), "%" + surname.toLowerCase() + "%")
        );
      } else if (name != null) {
        return cb.like(cb.lower(userJoin.get("name")), "%" + name.toLowerCase() + "%");
      } else {
        return cb.like(cb.lower(userJoin.get("surname")), "%" + surname.toLowerCase() + "%");
      }
    };
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
