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

