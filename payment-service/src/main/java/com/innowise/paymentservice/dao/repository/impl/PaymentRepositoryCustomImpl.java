package com.innowise.paymentservice.dao.repository.impl;

import com.innowise.paymentservice.dao.repository.PaymentRepositoryCustom;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

  private final MongoTemplate mongoTemplate;
  private static final String TIMESTAMP_FILTER = "timestamp";
  private static final String STATUS_FILTER = "status";

  @Override
  public List<Payment> findByFilters(UUID userId, UUID orderId, Status status, LocalDateTime from,
      LocalDateTime to) {
    var criteriaList = buildCommonCriteria(userId, orderId, status, from, to);

    Query query = new Query();
    if (!criteriaList.isEmpty()) {
      query.addCriteria(new Criteria().andOperator(criteriaList));
    }
    return mongoTemplate.find(query, Payment.class);
  }

  @Override
  public BigDecimal getTotalForUser(UUID userId, LocalDateTime from, LocalDateTime to) {

    var criteriaList = buildCommonCriteria(userId, null, Status.SUCCESS, from, to);
    return sumPaymentAmount(criteriaList);
  }

  @Override
  public BigDecimal getTotalForAll() {
    var criteriaList = buildCommonCriteria(null, null, Status.SUCCESS, null, null);
    return sumPaymentAmount(criteriaList);
  }

  private List<Criteria> buildCommonCriteria(UUID userId, UUID orderId, Status status,
      LocalDateTime from, LocalDateTime to) {
    var criteriaList = new ArrayList<Criteria>();
    if (userId != null) {
      criteriaList.add(Criteria.where("userId").is(userId));
    }
    if (orderId != null) {
      criteriaList.add(Criteria.where("orderId").is(orderId));
    }
    if (status != null) {
      criteriaList.add(Criteria.where(STATUS_FILTER).is(status));
    }
    if (from != null && to != null) {
      criteriaList.add(Criteria.where(TIMESTAMP_FILTER).gte(from).lte(to));
    } else if (from != null) {
      criteriaList.add(Criteria.where(TIMESTAMP_FILTER).gte(from));
    } else if (to != null) {
      criteriaList.add(Criteria.where(TIMESTAMP_FILTER).lte(to));
    }
    return criteriaList;
  }

  private BigDecimal sumPaymentAmount(List<Criteria> criteriaList) {
    Aggregation aggregation = criteriaList.isEmpty()
        ? Aggregation.newAggregation(
        Aggregation.group().sum("paymentAmount").as("total"))
        : Aggregation.newAggregation(
            Aggregation.match(new Criteria().andOperator(criteriaList)),
            Aggregation.group().sum("paymentAmount").as("total"));

    AggregationResults<SumResult> results = mongoTemplate.aggregate(
        aggregation, "payments", SumResult.class);

    SumResult result = results.getUniqueMappedResult();
    return result != null ? result.getTotal() : BigDecimal.ZERO;
  }

}

