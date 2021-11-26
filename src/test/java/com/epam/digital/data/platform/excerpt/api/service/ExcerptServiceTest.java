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

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.epam.digital.data.platform.integration.ceph.dto.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcerptServiceTest {

  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  static final String BUCKET = "BUCKET";
  static final byte[] CEPH_CONTENT = "test".getBytes();

  ExcerptService instance;

  @Mock
  RecordRepository recordRepository;

  @Mock
  TemplateRepository templateRepository;

  @Mock
  KafkaHelper kafkaHelper;

  @Mock
  CephService excerptCephService;

  @Mock
  JwtHelper jwtHelper;

  @Mock
  DigitalSignatureService digitalSignatureService;

  @BeforeEach
  void setup() {
    instance = new ExcerptService(recordRepository, templateRepository, kafkaHelper,
        jwtHelper, excerptCephService, BUCKET, digitalSignatureService);
  }

  @Nested
  class Get {

    @Test
    void failWhenCephServiceFails() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");
      record.setExcerptKey("ceph-key");

      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(Optional.empty());
      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");

      assertThrows(ExcerptNotFoundException.class,
          () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void failWhenKeycloakIdDoesNotMatch() {
      var record = new ExcerptRecord();
      record.setKeycloakId("incorrectId");

      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");

      assertThrows(InvalidKeycloakIdException.class,
          () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void failWhenExcerptCephKeyIsNull() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");

      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");

      assertThrows(ExcerptNotFoundException.class,
          () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void failWhenNoRecordFound() {
      assertThrows(ExcerptNotFoundException.class, () -> instance.getExcerpt(ID, any()));
    }

    @Test
    void failWhenNotFoundInCeph() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");
      record.setExcerptKey("ceph-key");

      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");
      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(Optional.empty());

      assertThrows(ExcerptNotFoundException.class,
          () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void returnResource() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");
      record.setExcerptKey("ceph-key");

      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");
      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(
          Optional.of(new CephObject(CEPH_CONTENT, Map.of())));

      var resource = instance.getExcerpt(ID, securityContext());

      assertThat(resource.getByteArray()).isEqualTo(CEPH_CONTENT);
    }
  }

  @Nested
  class Status {

    @Test
    void failWhenRecordNotFound() {
      assertThrows(ExcerptNotFoundException.class, () -> instance.getStatus(ID));
    }

    @Test
    void returnStatus() {
      var expectedStatus = FAILED;
      var expectedDetails = "some details";
      setupStatusFound(expectedStatus, expectedDetails);

      var status = instance.getStatus(ID);

      assertThat(status.getStatus()).isEqualTo(expectedStatus);
      assertThat(status.getStatusDetails()).isEqualTo(expectedDetails);
    }
  }

  @Nested
  class Generate {

    @Test
    void failWhenTemplateTypeNotFound() {
      assertThrows(ExcerptProcessingException.class,
          () -> instance.generateExcerpt(buildExcerptEvent(), requestContext(), securityContext()));
    }

    @Test
    void returnEntityId() {
      setupExcerptFound(ID);

      var entityId = instance.generateExcerpt(buildExcerptEvent(), requestContext(),
          securityContext());

      verify(recordRepository).save(any());
      verify(kafkaHelper).send(any(), any(), any());
      assertThat(entityId.getExcerptIdentifier()).isEqualTo(ID);
    }

    @Test
    void exceptionIfHeadersAbsent() {

      var expectedMessage = "Mandatory header(s) missed: [X-Digital-Signature, X-Digital-Signature-Derived]";

      String actualMessage = null;
      try {
        instance.generateExcerpt(buildExcerptEvent(), requestContext(), new SecurityContext());
      } catch (MandatoryHeaderMissingException e) {
        actualMessage = e.getMessage();
      }
      assertEquals(expectedMessage, actualMessage);
    }
  }

  private void setupStatusFound(ExcerptProcessingStatus status, String details) {
    var record = new ExcerptRecord();
    record.setStatus(status);
    record.setStatusDetails(details);
    when(recordRepository.findById(any())).thenReturn(Optional.of(record));
  }

  private void setupExcerptFound(UUID id) {
    var record = new ExcerptRecord();
    record.setId(id);
    when(templateRepository.findFirstByTemplateName("test_type")).thenReturn(
        Optional.of(new ExcerptTemplate()));
    when(recordRepository.save(any())).thenReturn(record);
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
