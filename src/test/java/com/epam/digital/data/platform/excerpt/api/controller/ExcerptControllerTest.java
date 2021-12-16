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

package com.epam.digital.data.platform.excerpt.api.controller;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.excerpt.api.service.ExcerptService;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import com.epam.digital.data.platform.excerpt.model.StatusDto;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ExcerptController.class)
class ExcerptControllerTest {

  static final String BASE_URL = "/excerpts";
  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  static final long EXCERPT_CONTENT_LENGTH = 4;

  static final String CONTENT_DISPOSITION_HEADER_NAME = "Content-Disposition";
  static final String CONTENT_DISPOSITION_HEADER_VALUE = "attachment; filename=\"123e4567-e89b-12d3-a456-426614174000.pdf\"";
  static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  ExcerptService excerptService;

  @Test
  void getStatus() throws Exception {
    var expectedStatus = FAILED;
    var expectedDetails = "some details";
    when(excerptService.getStatus(any()))
        .thenReturn(new StatusDto(expectedStatus, expectedDetails));

    mockMvc.perform(get(BASE_URL + "/{id}/status", ID))
        .andExpect(matchAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.status", is(expectedStatus.toString())),
            jsonPath("$.statusDetails", is(expectedDetails)))
        );
  }

  @Test
  void getGeneratedId() throws Exception {
    when(excerptService.generateExcerpt(any(), any(), any()))
        .thenReturn(new ExcerptEntityId(ID));

    mockMvc.perform(post(BASE_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            "{\"excerptType\": \"test_type\","
              + "\"excerptInputData\": {\"field\": \"data\"},"
              + "\"requiresSystemSignature\": false}"
        ))
        .andExpect(matchAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.excerptIdentifier", is(ID.toString())))
        );
  }

  @Test
  void getExcerpt() throws Exception {
    InputStream excerptContent = new ByteArrayInputStream("test".getBytes());
    var excerpt = CephObject.builder()
            .content(excerptContent)
            .metadata(CephObjectMetadata.builder()
                    .contentLength(EXCERPT_CONTENT_LENGTH)
                    .build())
            .build();
    when(excerptService.getExcerpt(any(), any()))
        .thenReturn(excerpt);

    mockMvc.perform(get(BASE_URL + "/{id}", ID))
        .andExpect(matchAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_OCTET_STREAM),
            header().longValue(CONTENT_LENGTH_HEADER_NAME, EXCERPT_CONTENT_LENGTH),
            header().string(CONTENT_DISPOSITION_HEADER_NAME, CONTENT_DISPOSITION_HEADER_VALUE))
        );
  }
}
