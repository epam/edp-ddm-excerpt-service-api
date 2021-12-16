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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dso.api.dto.ErrorDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.excerpt.api.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidSignatureException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSignatureServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String REQUEST_BUCKET = "REQUEST_BUCKET";
  private static final String EXCERPT_BUCKET = "EXCERPT_BUCKET";
  private static final String X_DIG_SIG_DERIVED = "xDigitalSignatureHeaderDerived";
  private static final String RESPONSE_FROM_CEPH = "{\"signature\":\"signature\",\"data\":\"\"}";
  private static final String SIGNATURE = "signature";
  private static final ErrorDto errorDto = ErrorDto.builder().message("error_message").build();

  private static final ExcerptEventDto DATA_OBJ = new ExcerptEventDto(
      UUID.fromString("11111111-1111-1111-1111-111111111111"),
      "TestExcerpt",
      Map.of("test_key", "test_value"),
      true);

  private static String DATA_STR;

  @Mock
  private CephService requestCephService;
  @Mock
  private CephService excerptCephService;
  @Mock
  private DigitalSealRestClient digitalSealRestClient;

  @Captor
  private ArgumentCaptor<VerifyRequestDto> requestCaptor;

  private DigitalSignatureService digitalSignatureService;

  @BeforeAll
  static void setup() throws JsonProcessingException {
    DATA_STR = OBJECT_MAPPER.writeValueAsString(DATA_OBJ);
  }

  @BeforeEach
  void init() {
    digitalSignatureService = new DigitalSignatureService(requestCephService, excerptCephService,
        REQUEST_BUCKET, EXCERPT_BUCKET, digitalSealRestClient, OBJECT_MAPPER);

    lenient().when(requestCephService.getAsString(REQUEST_BUCKET, X_DIG_SIG_DERIVED))
        .thenReturn(Optional.of(RESPONSE_FROM_CEPH));
  }

  @Test
  void validSignatureTest() {
    when(digitalSealRestClient.verify(any())).thenReturn(new VerifyResponseDto(true, null));

    digitalSignatureService.checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED);

    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals(SIGNATURE, requestCaptor.getValue().signature());
    assertEquals(DATA_STR, requestCaptor.getValue().data());
  }

  @Test
  void shouldThrowInvalidSignatureException() {
    when(digitalSealRestClient.verify(any())).
        thenReturn(new VerifyResponseDto(false, errorDto));

    String exceptionMessage = null;
    try {
      digitalSignatureService.checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED);
    } catch (InvalidSignatureException e) {
      exceptionMessage = e.getMessage();
    }

    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals("error_message", exceptionMessage);
    assertEquals(SIGNATURE, requestCaptor.getValue().signature());
    assertEquals(DATA_STR, requestCaptor.getValue().data());
  }

  @Test
  void cephServiceThrowsExceptionTest() {
    when(requestCephService.getAsString(REQUEST_BUCKET, X_DIG_SIG_DERIVED))
        .thenThrow(new CephCommunicationException("", new RuntimeException()));

    assertThrows(CephCommunicationException.class,
        () -> digitalSignatureService.checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED));
  }

  @Test
  void shouldThrowExceptionWhenSignatureNotFoundInCheckSignature() {
    when(requestCephService.getAsString(REQUEST_BUCKET, X_DIG_SIG_DERIVED))
        .thenReturn(Optional.empty());

    assertThrows(DigitalSignatureNotFoundException.class,
        () -> digitalSignatureService.checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED));
  }

  @Test
  void shouldThrowExceptionWhenSignatureNotFoundInSaveSignature() {
    when(requestCephService.getAsString(REQUEST_BUCKET, X_DIG_SIG_DERIVED))
        .thenReturn(Optional.empty());

    assertThrows(DigitalSignatureNotFoundException.class,
        () -> digitalSignatureService.saveSignature(X_DIG_SIG_DERIVED));
  }

  @Test
  void badRequestExceptionChangedToKepServiceBadRequestException() {
    when(digitalSealRestClient.verify(any())).thenThrow(BadRequestException.class);

    assertThrows(KepServiceBadRequestException.class, () -> digitalSignatureService
        .checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED));
  }

  @Test
  void internalServerErrorExceptionChangedToKepServiceInternalServerErrorException() {
    when(digitalSealRestClient.verify(any())).thenThrow(InternalServerErrorException.class);

    assertThrows(KepServiceInternalServerErrorException.class, () -> digitalSignatureService
        .checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED));
  }

  @Test
  void shouldCallMethodsWithAppropriateParameters() {

    String result = digitalSignatureService.saveSignature(X_DIG_SIG_DERIVED);

    verify(requestCephService).getAsString(REQUEST_BUCKET, X_DIG_SIG_DERIVED);
    verify(excerptCephService).put(EXCERPT_BUCKET, X_DIG_SIG_DERIVED, RESPONSE_FROM_CEPH);
    assertEquals(RESPONSE_FROM_CEPH, result);
  }

  @Test
  void jsonProcessingExceptionChangedToRuntimeException() {
    when(requestCephService.getAsString(any(),any())).thenReturn(Optional.of("{{"));

    assertThrows(RuntimeException.class,
        () -> digitalSignatureService.checkSignature(DATA_OBJ, X_DIG_SIG_DERIVED));
  }
}