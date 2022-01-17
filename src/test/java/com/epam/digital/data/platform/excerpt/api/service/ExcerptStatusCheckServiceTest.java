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
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcerptStatusCheckServiceTest {

  private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  private ExcerptStatusCheckService instance;

  @Mock
  private RecordRepository recordRepository;

  @BeforeEach
  void beforeEach() {
    instance = new ExcerptStatusCheckService(recordRepository);
  }

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

  private void setupStatusFound(ExcerptProcessingStatus status, String details) {
    var record = new ExcerptRecord();
    record.setStatus(status);
    record.setStatusDetails(details);
    when(recordRepository.findById(any())).thenReturn(Optional.of(record));
  }
}
