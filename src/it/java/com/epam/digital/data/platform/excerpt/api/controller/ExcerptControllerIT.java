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

import static com.epam.digital.data.platform.excerpt.api.TestUtils.readClassPathResource;
import static com.epam.digital.data.platform.excerpt.api.util.Header.ACCESS_TOKEN;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_SOURCE_APPLICATION;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_SOURCE_BUSINESS_ACTIVITY;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_SOURCE_BUSINESS_PROCESS;
import static com.epam.digital.data.platform.excerpt.api.util.Header.X_SOURCE_SYSTEM;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.excerpt.api.BaseIT;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ExcerptControllerIT extends BaseIT {

  @Autowired
  JwtHelper jwtHelper;

  @MockBean(name = "excerptCephService")
  CephService excerptCephService;

  @Mock
  CephObject cephObject;
  @Mock
  CephObjectMetadata cephObjectMetadata;

  private static String OFFICER_TOKEN;

  @BeforeAll
  static void init() throws IOException {
    OFFICER_TOKEN = readClassPathResource("/officerToken.txt");
  }

  @Test
  void shouldCreateExcerptRecordInDatabase() throws Exception {

    // given
    var template = readClassPathResource("/template.ftl");
    saveExcerptTemplateToDatabase("template", template);

    var requestJson = readClassPathResource("/json/request.json");

    // when
    ResultActions resultActions = mockMvc.perform(
            post("/excerpts")
                .header(ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN)
                .header(X_DIGITAL_SIGNATURE.getHeaderName(), "X_DIGITAL_SIGNATURE")
                .header(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName(), "X_DIGITAL_SIGNATURE_DERIVED")
                .header(X_SOURCE_SYSTEM.getHeaderName(), "X_SOURCE_SYSTEM")
                .header(X_SOURCE_APPLICATION.getHeaderName(), "X_SOURCE_APPLICATION")
                .header(X_SOURCE_BUSINESS_PROCESS.getHeaderName(), "X_SOURCE_BUSINESS_PROCESS")
                .header(X_SOURCE_BUSINESS_ACTIVITY.getHeaderName(), "X_SOURCE_BUSINESS_ACTIVITY")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.excerptIdentifier").exists());

    // then
    var id = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsString(),
        ExcerptEntityId.class).getExcerptIdentifier();

    ExcerptRecord excerptRecord = recordRepository.findById(id).get();

    assertThat(excerptRecord.getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(excerptRecord.getxDigitalSignature()).isEqualTo("X_DIGITAL_SIGNATURE");
    assertThat(excerptRecord.getxDigitalSignatureDerived()).isEqualTo(
        "X_DIGITAL_SIGNATURE_DERIVED");
    assertThat(excerptRecord.getxSourceSystem()).isEqualTo("X_SOURCE_SYSTEM");
    assertThat(excerptRecord.getxSourceApplication()).isEqualTo("X_SOURCE_APPLICATION");
    assertThat(excerptRecord.getxSourceBusinessProcess()).isEqualTo("X_SOURCE_BUSINESS_PROCESS");
    assertThat(excerptRecord.getxSourceBusinessActivity()).isEqualTo("X_SOURCE_BUSINESS_ACTIVITY");

    assertThat(excerptRecord.getKeycloakId()).isNotBlank();
    assertThat(excerptRecord.getExcerptKey()).isNull();
    assertThat(excerptRecord.getCreatedAt()).isNotNull();
    assertThat(excerptRecord.getUpdatedAt()).isNotNull();
  }

  @Test
  void returnErrorWhenTemplateNotFound() throws Exception {
    var requestJson = readClassPathResource("/json/request.json");

    mockMvc.perform(
            post("/excerpts")
                .header(ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN)
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.statusDetails").value("Template not found: template"));
  }

  @Test
  void shouldFindExcerptRecordInDatabase() throws Exception {

    // given
    String resultExcerpt = "Result excerpt";

    when(excerptCephService.get("bucket", "11111111-1111-1111-1111-111111111111"))
        .thenReturn(Optional.ofNullable(cephObject));
    when(cephObject.getMetadata()).thenReturn(cephObjectMetadata);
    when(cephObject.getContent()).thenReturn(new ByteArrayInputStream(resultExcerpt.getBytes()));
    when(cephObjectMetadata.getContentLength()).thenReturn(14L);

    ExcerptRecord excerptRecord = new ExcerptRecord();
    excerptRecord.setKeycloakId(jwtHelper.getKeycloakId(OFFICER_TOKEN));
    excerptRecord.setExcerptKey("11111111-1111-1111-1111-111111111111");

    excerptRecord = saveExcerptRecordToDatabase(excerptRecord);

    // when
    var mvcResult = mockMvc.perform(get("/excerpts/" + excerptRecord.getId())
            .header(ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    // then
    assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(resultExcerpt);
  }

  @Test
  void shouldNotFindExcerptRecordInDatabase() throws Exception {
    mockMvc.perform(get("/excerpts/11111111-1111-1111-1111-111111111111")
            .header(ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  void unauthorizedIfAccessTokenAbsent() throws Exception {
    mockMvc.perform(get("/excerpts/11111111-1111-1111-1111-111111111111"))
        .andExpect(status().isUnauthorized());
  }
}
