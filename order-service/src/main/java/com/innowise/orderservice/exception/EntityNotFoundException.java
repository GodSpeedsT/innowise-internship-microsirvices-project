package com.innowise.orderservice.exception;

import java.util.UUID;

public class EntityNotFoundException extends OrderServiceException {

  public EntityNotFoundException(String entityName, UUID id) {
    super(entityName + " not found with id: " + id);
  }

  public EntityNotFoundException(String entityName, String field, String value) {
    super(entityName + " not found with " + field + ": " + value);
  }
}
