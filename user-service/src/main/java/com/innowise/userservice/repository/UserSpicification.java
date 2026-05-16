package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;


public class UserSpicification {

  public static Specification<User> filterByUsernameAndSurname(String username, String surname) {
    return ((root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (username != null && !username.isBlank()) {
        predicates.add(criteriaBuilder.like(
            criteriaBuilder.lower(root.get("username")),
            "%" + username.toLowerCase() + "%"
        ));
      }
      if (surname != null && !surname.isBlank()) {
        predicates.add(criteriaBuilder.like(
            criteriaBuilder.lower(root.get("surname")),
            "%" + surname.toLowerCase() + "%"
        ));
      }
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    });
  }

  public static Specification<User> filterByActive(Boolean active) {
    return ((root, query, criteriaBuilder) -> {
      if (active == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("active"), active);
    });
  }

}
