package com.innowise.userservice.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public static ResourceNotFoundException ofUser(UUID userId) {
    return new ResourceNotFoundException("User with id " + userId + " not found");
  }

  public static ResourceNotFoundException ofCard(UUID id) {
    return new ResourceNotFoundException("Card with id " + id + " not found");
  }

}
