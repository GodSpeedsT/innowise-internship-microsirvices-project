package com.innowise.orderservice.exception;

public class DuplicateEntityException extends RuntimeException {

  public DuplicateEntityException(String entityName, String field, String value) {
    super(entityName + " already exists with " + field + ": " + value);
  }
}
