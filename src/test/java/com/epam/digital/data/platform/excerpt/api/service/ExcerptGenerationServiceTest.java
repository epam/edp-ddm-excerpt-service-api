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

package com.epam.digital.data.platform.excerpt.api.service;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcerptGenerationServiceTest {

  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  ExcerptGenerationService instance;

  @Mock
  RecordRepository recordRepository;
  @Mock
  TemplateRepository templateRepository;
  @Mock
  KafkaHelper kafkaHelper;
  @Mock
  JwtHelper jwtHelper;
  @Mock
  DigitalSignatureService digitalSignatureService;

  @BeforeEach
  void setup() {
    instance =
        new ExcerptGenerationService(
            recordRepository,
            templateRepository,
            kafkaHelper,
            jwtHelper,
            digitalSignatureService,
            true);
  }

  @Test
  void failWhenTemplateTypeNotFound() {
    assertThrows(
        ExcerptProcessingException.class,
        () -> instance.generateExcerpt(buildExcerptEvent(), requestContext(), securityContext()));
  }

  @Test
  void returnEntityId() {
    setupExcerptFound();

    var entityId =
        instance.generateExcerpt(buildExcerptEvent(), requestContext(), securityContext());

    verify(recordRepository).save(any());
    verify(kafkaHelper).send(any(), any(), any(), any());
    assertThat(entityId.getExcerptIdentifier()).isEqualTo(ID);
  }

  @Test
  void exceptionIfHeadersAbsent() {

    var expectedMessage =
        "Mandatory header(s) missed: [X-Digital-Signature, X-Digital-Signature-Derived]";

    String actualMessage = null;
    try {
      instance.generateExcerpt(buildExcerptEvent(), requestContext(), new SecurityContext());
    } catch (MandatoryHeaderMissingException e) {
      actualMessage = e.getMessage();
    }
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void expectDigitalSignaturesNotProcessedIfDisabled() {
    instance =
        new ExcerptGenerationService(
            recordRepository,
            templateRepository,
            kafkaHelper,
            jwtHelper,
            digitalSignatureService,
            false);
    setupExcerptFound();

    var mockExcerptEvent = buildExcerptEvent();
    instance.generateExcerpt(mockExcerptEvent, requestContext(), securityContext());

    verifyNoInteractions(digitalSignatureService);
  }

  private void setupExcerptFound() {
    var record = new ExcerptRecord();
    record.setId(ID);
    when(recordRepository.save(any())).thenReturn(record);
    
    var template = new ExcerptTemplate();
    template.setTemplateType("pdf");
    when(templateRepository.findFirstByTemplateName("test_type"))
        .thenReturn(Optional.of(template));
  }

  private ExcerptEventDto buildExcerptEvent() {
    return new ExcerptEventDto(ID, "test_type", new HashMap<>(), false);
  }

  private SecurityContext securityContext() {
    var context = new SecurityContext();
    context.setAccessToken("stub");
    context.setDigitalSignature("digital_signature");
    context.setDigitalSignatureDerived("digital_signature_derived");
    return context;
  }

  private RequestContext requestContext() {
    var context = new RequestContext();
    context.setSourceSystem("source_system");
    context.setSourceApplication("source_application");
    context.setSourceBusinessActivity("business_activity");
    context.setSourceBusinessProcess("business_process");
    return context;
  }
}
