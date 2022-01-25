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
import com.epam.digital.data.platform.starter.audit.model.AuditEvent;
import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TokenParser.class, ObjectMapper.class})
class RestAuditEventsFacadeTest {

  private static final String APP_NAME = "application";
  private static final String REQUEST_ID = "1";
  private static final String SOURCE_SYSTEM = "system";
  private static final String SOURCE_APPLICATION = "source_app";
  private static final String BUSINESS_PROCESS = "bp";
  private static final String BUSINESS_PROCESS_DEFINITION_ID = "bp_def_id";
  private static final String BUSINESS_PROCESS_INSTANCE_ID = "bp_id";
  private static final String BUSINESS_ACTIVITY = "act";
  private static final String BUSINESS_ACTIVITY_INSTANCE_ID = "bp_act";
  private static final String METHOD_NAME = "method";
  private static final String ACTION = "CREATE";
  private static final String STEP = "BEFORE";
  private static final String USER_DRFO = "1010101014";
  private static final String USER_KEYCLOAK_ID = "496fd2fd-3497-4391-9ead-41410522d06f";
  private static final String USER_NAME = "Сидоренко Василь Леонідович";
  private static final String RESULT = "RESULT";

  private static final LocalDateTime CURR_TIME = LocalDateTime.of(2021, 4, 1, 11, 50);

  private RestAuditEventsFacade restAuditEventsFacade;
  private static String ACCESS_TOKEN;

  @Mock
  private AuditService auditService;
  @Mock
  private AuditSourceInfoProvider auditSourceInfoProvider;
  @Mock
  private TraceProvider traceProvider;
  @Autowired
  private TokenParser tokenParser;

  private final Clock clock =
      Clock.fixed(CURR_TIME.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  @Captor
  private ArgumentCaptor<AuditEvent> auditEventCaptor;

  private AuditSourceInfo mockSourceInfo;

  @BeforeAll
  static void init() throws IOException {
    ACCESS_TOKEN = new String(IOUtils.toByteArray(
        RestAuditEventsFacadeTest.class.getResourceAsStream("/accessToken.json")));
  }

  @BeforeEach
  void beforeEach() {
    restAuditEventsFacade =
        new RestAuditEventsFacade(
            auditService, APP_NAME, clock, traceProvider, auditSourceInfoProvider, tokenParser);

    when(traceProvider.getRequestId()).thenReturn(REQUEST_ID);

    mockSourceInfo =
        AuditSourceInfo.AuditSourceInfoBuilder.anAuditSourceInfo()
            .system(SOURCE_SYSTEM)
            .application(SOURCE_APPLICATION)
            .businessProcess(BUSINESS_PROCESS)
            .businessProcessDefinitionId(BUSINESS_PROCESS_DEFINITION_ID)
            .businessProcessInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
            .businessActivity(BUSINESS_ACTIVITY)
            .businessActivityInstanceId(BUSINESS_ACTIVITY_INSTANCE_ID)
            .build();
    when(auditSourceInfoProvider.getAuditSourceInfo())
            .thenReturn(mockSourceInfo);
  }

  @Test
  void expectCorrectAuditEventWithIdAndJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP, "row_id", 54, "result", RESULT);
    when(auditService.createContext(ACTION, STEP, null, "54", null, RESULT)).thenReturn(context);
    when(traceProvider.getAccessToken()).thenReturn(ACCESS_TOKEN);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, STEP, 54, RESULT);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("HTTP request. Method: method")
            .requestId(REQUEST_ID)
            .sourceInfo(mockSourceInfo)
            .userInfo(createUserInfo())
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();

    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  @Test
  void expectCorrectAuditEventWithoutIdAndJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP);
    when(auditService.createContext(ACTION, STEP, null, null, null, null))
        .thenReturn(context);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, STEP, null, null);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("HTTP request. Method: method")
            .requestId(REQUEST_ID)
            .sourceInfo(mockSourceInfo)
            .userInfo(null)
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();

    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  @Test
  void expectExceptionAuditWithUserInfo() {
    when(traceProvider.getAccessToken()).thenReturn(ACCESS_TOKEN);
    Map<String, Object> context = Map.of("action", ACTION);
    when(auditService.createContext(ACTION, null, null, null, null, null))
        .thenReturn(context);

    var exceptionAuditEvent = new ExceptionAuditEvent();
    exceptionAuditEvent.setEventType(EventType.USER_ACTION);
    exceptionAuditEvent.setAction(ACTION);
    exceptionAuditEvent.setUserInfoEnabled(true);

    restAuditEventsFacade.sendExceptionAudit(exceptionAuditEvent);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("EXCEPTION")
            .requestId(REQUEST_ID)
            .sourceInfo(mockSourceInfo)
            .userInfo(createUserInfo())
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();

    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  private AuditUserInfo createUserInfo() {
    return new AuditUserInfo(USER_NAME, USER_KEYCLOAK_ID, USER_DRFO);
  }
}