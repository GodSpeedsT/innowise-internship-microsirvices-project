package com.innowise.orderservice.exception;

public class ExternalServiceException extends OrderServiceException {

  public ExternalServiceException(String serviceName, Throwable cause) {
    super("Failed to communicate with " + serviceName, cause);
  }

  public ExternalServiceException(String message) {
    super(message);
  }
}
