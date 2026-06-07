package com.innowise.orderservice.exception;

public class OrderServiceException extends RuntimeException {

  protected OrderServiceException(String message) {
    super(message);
  }

  protected OrderServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
