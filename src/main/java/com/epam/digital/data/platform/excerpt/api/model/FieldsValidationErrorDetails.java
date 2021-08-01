package com.epam.digital.data.platform.excerpt.api.model;

import java.util.List;

public class FieldsValidationErrorDetails {

  private final List<FieldError> errors;

  public FieldsValidationErrorDetails(List<FieldError> errors) {
    this.errors = errors;
  }

  public List<FieldError> getErrors() {
    return errors;
  }

  public static class FieldError {

    private final Object value;
    private final String field;
    private final String message;

    public FieldError(String message) {
      this(null, null, message);
    }

    public FieldError(Object value, String field, String message) {
      this.value = value;
      this.field = field;
      this.message = message;
    }

    public Object getValue() {
      return value;
    }

    public String getField() {
      return field;
    }

    public String getMessage() {
      return message;
    }
  }
}
