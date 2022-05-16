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

package com.epam.digital.data.platform.excerpt.api.exception;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@WebMvcTest
@ContextConfiguration(classes = {MockController.class, ApplicationExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ApplicationExceptionHandlerTest extends ResponseEntityExceptionHandler {

  private static final String BASE_URL = "/mock";
  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  private static final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";
  private static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";

  private static final String VALID_INPUT =
      "{\"recordId\":\"6d34cbd7-dded-495a-a1c6-4f37d823b59d\","
          + "\"excerptType\":\"validtype\",\"updatedAt\":\"2021-07-29T11:52:39.972053\"}";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private MockService mockService;

  @Test
  void shouldReturn500ThirdPartyServiceUnavailableWhenCephCommunicationException()
      throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(CephCommunicationException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("THIRD_PARTY_SERVICE_UNAVAILABLE")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof CephCommunicationException));
  }

  @Test
  void shouldReturn500InternalContractViolationWhenMisconfigurationException() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(MisconfigurationException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("INTERNAL_CONTRACT_VIOLATION")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof MisconfigurationException));
  }

  @Test
  void shouldReturn500ThirdPartyServiceUnavailableWhenKepServiceInternalServerErrorException()
      throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(
        KepServiceInternalServerErrorException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("THIRD_PARTY_SERVICE_UNAVAILABLE")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof KepServiceInternalServerErrorException));
  }

  @Test
  void shouldReturn500InternalContractViolationWhenKepServiceBadRequestException()
      throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(KepServiceBadRequestException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("INTERNAL_CONTRACT_VIOLATION")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof KepServiceBadRequestException));
  }

  @Test
  void shouldReturn412SignatureViolationWhenInvalidSignatureException() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(InvalidSignatureException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isPreconditionFailed())
        .andExpectAll(
            jsonPath("$.code").value(is("SIGNATURE_VIOLATION")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof InvalidSignatureException));
  }

  @Test
  void shouldReturn400InvalidHeaderValueWhenDigitalSignatureNotFoundException() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(DigitalSignatureNotFoundException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            jsonPath("$.code").value(is("INVALID_HEADER_VALUE")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof DigitalSignatureNotFoundException));
  }

  @Test
  void shouldReturn400SigningNotAllowedValueWhenSigningNotAllowedException() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(SigningNotAllowedException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            jsonPath("$.code").value(is("SIGNING_NOT_ALLOWED")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof SigningNotAllowedException));
  }

  @Test
  void shouldReturn400HeadersAreMissingWhenMandatoryHeaderMissingException() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(MandatoryHeaderMissingException.class);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_INPUT))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            jsonPath("$.code").value(is("HEADERS_ARE_MISSING")),
            jsonPath("$.details").doesNotExist())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof MandatoryHeaderMissingException));
  }

  @Test
  void shouldReturnNotFoundWhenNoSuchElementException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(ExcerptNotFoundException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isNotFound())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof ExcerptNotFoundException))
        .andExpectAll(
            jsonPath("$.code").value(is("NOT_FOUND")),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestWhenExcerptProcessingException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(new ExcerptProcessingException(FAILED, "test"));

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isBadRequest())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof ExcerptProcessingException))
        .andExpectAll(
            jsonPath("$.status").value(is("FAILED")),
            jsonPath("$.statusDetails").value(is("test")));
  }

  @Test
  void shouldReturnRuntimeErrorOnGenericException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(RuntimeException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("RUNTIME_ERROR")),
            jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestOnInvalidKeycloakIdException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(InvalidKeycloakIdException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            jsonPath("$.code").value(is("INVALID_KEYCLOAK_ID")),
            jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestOnHttpNotReadable() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(HttpMessageNotReadableException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isBadRequest())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof HttpMessageNotReadableException));
  }

  @Test
  void shouldReturnUnprocessableEntityWhenJSONValueIsInvalid() throws Exception {
    when(mockService.generateExcerpt(any())).thenThrow(HttpMessageNotReadableException.class);

    var inputString = "{\"recordId\":null,\"excerptType\":\"validtype\",\"updatedAt\":\"202ss1-07-29T11:52:39.972053\"}";

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(inputString))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof HttpMessageNotReadableException));
  }

  @Test
  void shouldReturn400WithBodyWhenPathArgumentIsNotValid() throws Exception {
    mockMvc.perform(get(BASE_URL + "/invalidUUID"))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is(METHOD_ARGUMENT_TYPE_MISMATCH)),
            jsonPath("$.details").doesNotExist()
        );
  }

  @Test
  void shouldReturn403WhenForbiddenOperation() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(AccessDeniedException.class);

    mockMvc
        .perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpectAll(
            status().isForbidden(),
            jsonPath("$.code").value(is(FORBIDDEN_OPERATION)));
  }

  @Test
  void shouldReturn422WithBodyWhenMethodArgumentNotValid() throws Exception {
    var inputBody = new MockEntity();
    inputBody.setRecordId(UUID.randomUUID());
    inputBody.setExcerptType("invalidtypename");
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    var expectedResponseObject = new DetailedErrorResponse<FieldsValidationErrorDetails>();
    expectedResponseObject.setCode("VALIDATION_ERROR");
    expectedResponseObject.setDetails(
        validationDetailsFrom(new FieldsValidationErrorDetails.FieldError(
            "invalidtypename", "excerptType", "must match \"^[a-zA-Z]{6,10}$\"")));

    var result = mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(inputStringBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(response ->
            assertTrue(response.getResolvedException() instanceof MethodArgumentNotValidException));

    var traceId = objectMapper.readValue(
            result.andReturn().getResponse().getContentAsString(), DetailedErrorResponse.class)
        .getTraceId();

    expectedResponseObject.setTraceId(traceId);
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    result.andExpect(content().json(expectedOutputBody));
  }

  public static FieldsValidationErrorDetails validationDetailsFrom(
      FieldsValidationErrorDetails.FieldError... details) {
    return new FieldsValidationErrorDetails(Arrays.asList(details));
  }
}
