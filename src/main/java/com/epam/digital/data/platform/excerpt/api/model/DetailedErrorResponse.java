package com.epam.digital.data.platform.excerpt.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class DetailedErrorResponse<T> {
  private String code;
  private T details;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @JsonInclude(Include.NON_NULL)
  public T getDetails() {
    return details;
  }

  public void setDetails(T details) {
    this.details = details;
  }
}
