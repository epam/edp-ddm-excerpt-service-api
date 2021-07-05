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

package com.epam.digital.data.platform.excerpt.api.model.audit;

import com.epam.digital.data.platform.starter.audit.model.EventType;

public class ExceptionAuditEvent {
  private EventType eventType;
  private String action;
  private boolean userInfoEnabled;

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public boolean isUserInfoEnabled() {
    return userInfoEnabled;
  }

  public void setUserInfoEnabled(boolean userInfoEnabled) {
    this.userInfoEnabled = userInfoEnabled;
  }
}
