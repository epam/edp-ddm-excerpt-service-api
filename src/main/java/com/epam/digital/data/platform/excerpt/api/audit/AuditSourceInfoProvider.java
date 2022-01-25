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

package com.epam.digital.data.platform.excerpt.api.audit;

import com.epam.digital.data.platform.excerpt.api.util.Header;
import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditSourceInfoProvider {

  public AuditSourceInfo getAuditSourceInfo() {
    return AuditSourceInfo.AuditSourceInfoBuilder.anAuditSourceInfo()
        .system(MDC.get(Header.X_SOURCE_SYSTEM.getHeaderName().toLowerCase()))
        .application(MDC.get(Header.X_SOURCE_APPLICATION.getHeaderName().toLowerCase()))
        .businessProcess(MDC.get(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName().toLowerCase()))
        .businessActivity(MDC.get(Header.X_SOURCE_BUSINESS_ACTIVITY.getHeaderName().toLowerCase()))
        .build();
  }
}
