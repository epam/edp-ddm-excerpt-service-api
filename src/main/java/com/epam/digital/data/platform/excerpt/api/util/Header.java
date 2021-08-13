package com.epam.digital.data.platform.excerpt.api.util;

public enum Header {
  TRACE_ID("X-B3-TraceId"),
  ACCESS_TOKEN("X-Access-Token"),
  X_DIGITAL_SIGNATURE("X-Digital-Signature"),
  X_DIGITAL_SIGNATURE_DERIVED("X-Digital-Signature-Derived");

  private final String headerName;

  Header(String headerName) {
    this.headerName = headerName;
  }

  public String getHeaderName() {
    return headerName;
  }
}
