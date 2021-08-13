package com.epam.digital.data.platform.excerpt.api.exception;

import java.util.List;

public class MandatoryHeaderMissingException extends RuntimeException {

  private final List<String> missed;

  public MandatoryHeaderMissingException(List<String> missed) {
    super("Mandatory header(s) missed");
    this.missed = missed;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + ": " + missed;
  }
}
