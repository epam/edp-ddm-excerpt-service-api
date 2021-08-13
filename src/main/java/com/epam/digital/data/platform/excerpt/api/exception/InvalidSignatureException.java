package com.epam.digital.data.platform.excerpt.api.exception;

public class InvalidSignatureException extends RuntimeException {

  public InvalidSignatureException(String msg) {
    super(msg);
  }
}
