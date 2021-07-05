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

import com.epam.digital.data.platform.excerpt.api.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.excerpt.api.service.TraceProvider;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class RestAuditEventsFacade extends AbstractAuditFacade {

  private final Logger log = LoggerFactory.getLogger(RestAuditEventsFacade.class);

  static final String HTTP_REQUEST = "HTTP request. Method: ";
  static final String EXCEPTION = "EXCEPTION";

  private final TraceProvider traceProvider;
  private final AuditSourceInfoProvider auditSourceInfoProvider;
  private final TokenParser tokenParser;

  public RestAuditEventsFacade(
      AuditService auditService,
      @Value("${spring.application.name:excerpt-service-api}") String appName,
      Clock clock,
      TraceProvider traceProvider,
      AuditSourceInfoProvider auditSourceInfoProvider,
      TokenParser tokenParser) {
    super(auditService, appName, clock);
    this.traceProvider = traceProvider;
    this.auditSourceInfoProvider = auditSourceInfoProvider;
    this.tokenParser = tokenParser;
  }

  public void sendExceptionAudit(ExceptionAuditEvent exceptionAuditEvent) {
    var event =
        createBaseAuditEvent(
                exceptionAuditEvent.getEventType(), EXCEPTION, traceProvider.getRequestId())
            .setSourceInfo(auditSourceInfoProvider.getAuditSourceInfo());

    var context =
        auditService.createContext(exceptionAuditEvent.getAction(), null, null, null, null, null);
    event.setContext(context);
    if (exceptionAuditEvent.isUserInfoEnabled()) {
      setUserInfoToEvent(event, traceProvider.getAccessToken());
    }

    log.debug("Sending Exception to Audit");
    auditService.sendAudit(event.build());
  }

  public void sendRestAudit(
      EventType eventType,
      String methodName,
      String action,
      String step,
      Object id,
      String result) {
    var event =
        createBaseAuditEvent(eventType, HTTP_REQUEST + methodName, traceProvider.getRequestId())
            .setSourceInfo(auditSourceInfoProvider.getAuditSourceInfo());

    var entityId = (id != null) ? id.toString() : null;
    var context = auditService.createContext(action, step, null, entityId, null, result);
    event.setContext(context);
    setUserInfoToEvent(event, traceProvider.getAccessToken());

    log.debug("Sending {} {} event to Audit", step, action);
    auditService.sendAudit(event.build());
  }

  private void setUserInfoToEvent(GroupedAuditEventBuilder event, String jwt) {
    if (jwt == null) {
      return;
    }

    var jwtClaimsDto = tokenParser.parseClaims(jwt);
    var userInfo = AuditUserInfo.AuditUserInfoBuilder.anAuditUserInfo()
            .userName(jwtClaimsDto.getFullName())
            .userKeycloakId(jwtClaimsDto.getSubject())
            .userDrfo(jwtClaimsDto.getDrfo())
            .build();
    event.setUserInfo(userInfo);
  }
}