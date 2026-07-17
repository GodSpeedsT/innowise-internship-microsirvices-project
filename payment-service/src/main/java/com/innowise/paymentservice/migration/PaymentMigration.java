package com.innowise.paymentservice.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "create-payments-collection", order = "001", author = "Kirill")
public class PaymentMigration {

  private static final String COLLECTION_NAME = "payments";

  @Execution
  public void execution(MongoTemplate mongoTemplate) {
    if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.createCollection(COLLECTION_NAME);
    }

  }

  @RollbackExecution
  public void rollbackExecution(MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(COLLECTION_NAME);
  }

}
