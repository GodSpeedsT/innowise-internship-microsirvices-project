package com.innowise.userservice.exception;

import java.io.Serial;

public class CardException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public CardException(String message) {
    super(message);
  }
}
