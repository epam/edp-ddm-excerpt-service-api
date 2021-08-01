package com.epam.digital.data.platform.excerpt.api.exception;

import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;

public class ExcerptProcessingException extends RuntimeException {

  private final ExcerptProcessingStatus status;
  private final String message;

  public ExcerptProcessingException(ExcerptProcessingStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public ExcerptProcessingStatus getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }
}

