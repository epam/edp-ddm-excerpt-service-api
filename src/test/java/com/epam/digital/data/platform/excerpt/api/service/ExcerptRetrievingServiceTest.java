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

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcerptRetrievingServiceTest {

  private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String BUCKET_NAME = "bucket";
  private static final String TOKEN = "token";

  private ExcerptRetrievingService instance;

  @Mock
  private RecordRepository recordRepository;
  @Mock
  private JwtHelper jwtHelper;
  @Mock
  private CephService excerptCephService;

  @BeforeEach
  void beforeEach() {
    instance =
        new ExcerptRetrievingService(recordRepository, jwtHelper, excerptCephService, BUCKET_NAME);
  }

  @Test
  void failWhenCephServiceFails() {
    var record = new ExcerptRecord();
    record.setKeycloakId("stubId");
    record.setExcerptKey("ceph-key");

    when(recordRepository.findById(any())).thenReturn(Optional.of(record));
    when(excerptCephService.get(any(), any())).thenReturn(Optional.empty());
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
    when(excerptCephService.get(any(), any())).thenReturn(Optional.empty());

    assertThrows(ExcerptNotFoundException.class,
            () -> instance.getExcerpt(ID, securityContext()));
  }

  @Test
  void returnResource() {
    var record = new ExcerptRecord();
    record.setKeycloakId("stubId");
    record.setExcerptKey("ceph-key");
    record.setExcerptType("type");

    when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");
    when(recordRepository.findById(any())).thenReturn(Optional.of(record));
    var cephServiceResponse = CephObject.builder()
            .content(new ByteArrayInputStream("test".getBytes()))
            .metadata(CephObjectMetadata.builder().userMetadata(new HashMap<>()).build())
            .build();
    when(excerptCephService.get(any(), any())).thenReturn(
            Optional.of(cephServiceResponse));

    var actualExcerptResponse = instance.getExcerpt(ID, securityContext());

    assertThat(actualExcerptResponse.getCephObject()).isEqualTo(cephServiceResponse);
    assertThat(actualExcerptResponse.getExcerptType()).isEqualTo("type");
  }

  private SecurityContext securityContext() {
    var context = new SecurityContext();
    context.setAccessToken(TOKEN);
    return context;
  }
}
