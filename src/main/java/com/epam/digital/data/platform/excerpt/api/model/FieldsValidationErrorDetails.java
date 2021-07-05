/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
