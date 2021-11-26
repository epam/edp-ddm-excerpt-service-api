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
import java.util.Base64;
import java.util.UUID;

import com.epam.digital.data.platform.excerpt.model.StatusDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ExcerptController.class)
class ExcerptControllerTest {

  static final String BASE_URL = "/excerpts";
  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  static final String ENCODED_STRING = Base64.getEncoder().encodeToString("test".getBytes());

  static final String HEADER_NAME = "Content-Disposition";
  static final String HEADER_VALUE = "attachment; filename=\"123e4567-e89b-12d3-a456-426614174000.pdf\"";

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
    when(excerptService.getExcerpt(any(), any()))
        .thenReturn(new ByteArrayResource(Base64.getDecoder().decode(ENCODED_STRING)));

    mockMvc.perform(get(BASE_URL + "/{id}", ID))
        .andExpect(matchAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_OCTET_STREAM),
            header().string(HEADER_NAME, HEADER_VALUE))
        );
  }
}
