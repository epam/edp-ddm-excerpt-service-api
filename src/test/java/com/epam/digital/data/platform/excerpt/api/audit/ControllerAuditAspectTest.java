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

import com.epam.digital.data.platform.excerpt.api.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.excerpt.api.exception.AuditException;
import com.epam.digital.data.platform.excerpt.api.exception.MockController;
import com.epam.digital.data.platform.excerpt.api.exception.MockEntity;
import com.epam.digital.data.platform.excerpt.api.exception.MockService;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.service.TraceProvider;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(AopAutoConfiguration.class)
@SpringBootTest(classes = {
    MockController.class,
    ControllerAuditAspectTest.MockNonControllerClient.class,
    ControllerAuditAspect.class,
    ApplicationExceptionHandler.class
})
@MockBean(ObjectMapper.class)
@MockBean(TraceProvider.class)
class ControllerAuditAspectTest {

  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockController controller;
  @Autowired
  private ApplicationExceptionHandler applicationExceptionHandler;
  @Autowired
  private MockNonControllerClient nonControllerClient;

  @MockBean
  private MockService mockService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;

  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private SecurityContext mockSecurityContext;

  @Test
  void expectAuditAspectBeforeAndAfterGetMethodWhenNoException() {
    when(mockService.getExcerpt(any())).thenReturn(new ByteArrayResource(new byte[] {}));

    controller.getExcerpt(ENTITY_ID);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnGetMethod() {
    when(mockService.getExcerpt(any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> controller.getExcerpt(ENTITY_ID));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectExceptionWhenControllerHasUnsupportedMappingAnnotation() {
    assertThrows(AuditException.class, () -> controller.deleteExcerpt(mockPayload()));
  }

  @Test
  void expectAuditAspectBeforeAndAfterPostMethodWhenNoException() {
    when(mockService.generateExcerpt(any())).thenReturn(new ExcerptEntityId());

    controller.generateExcerpt(mockPayload());

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnPostMethod() {
    when(mockService.generateExcerpt(any())).thenThrow(new RuntimeException());
    MockEntity mockEntity = mockPayload();

    assertThrows(RuntimeException.class, () -> controller.generateExcerpt(mockEntity));

    verify(restAuditEventsFacade).sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectNonCalledIfNonRestControllerCall() {
    nonControllerClient.postNonController();

    verifyNoInteractions(restAuditEventsFacade);
  }

  @Test
  void expectAuditAspectBeforeGetAndAfterExceptionHandler(){
    applicationExceptionHandler.handleException(new RuntimeException());

    verify(restAuditEventsFacade).sendExceptionAudit(any());
  }

  private MockEntity mockPayload() {
    MockEntity stub = new MockEntity();
    stub.setRecordId(ENTITY_ID);
    return stub;
  }

  @TestComponent
  public static class MockNonControllerClient {

    @PostMapping
    public MockEntity postNonController() {
      return new MockEntity();
    }
  }
}
