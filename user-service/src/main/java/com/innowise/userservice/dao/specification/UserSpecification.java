package com.innowise.userservice.dao.specification;

import com.innowise.userservice.entity.User;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;


public class UserSpecification {

  public static Specification<User> filterByNameAndSurname(String name, String surname) {
    return ((root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (name != null && !name.isBlank()) {
        predicates.add(criteriaBuilder.like(
            criteriaBuilder.lower(root.get("name")),
            "%" + name.toLowerCase() + "%"
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
