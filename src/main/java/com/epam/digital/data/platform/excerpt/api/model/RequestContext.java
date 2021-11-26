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

public class RequestContext {

  private String sourceSystem;
  private String sourceApplication;
  private String sourceBusinessProcess;
  private String sourceBusinessActivity;

  public String getSourceSystem() {
    return sourceSystem;
  }

  public void setSourceSystem(String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }

  public String getSourceApplication() {
    return sourceApplication;
  }

  public void setSourceApplication(String sourceApplication) {
    this.sourceApplication = sourceApplication;
  }

  public String getSourceBusinessProcess() {
    return sourceBusinessProcess;
  }

  public void setSourceBusinessProcess(String sourceBusinessProcess) {
    this.sourceBusinessProcess = sourceBusinessProcess;
  }

  public String getSourceBusinessActivity() {
    return sourceBusinessActivity;
  }

  public void setSourceBusinessActivity(String sourceBusinessActivity) {
    this.sourceBusinessActivity = sourceBusinessActivity;
  }
}
