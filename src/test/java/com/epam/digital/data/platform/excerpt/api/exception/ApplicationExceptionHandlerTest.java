package com.epam.digital.data.platform.excerpt.api.exception;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NoSuchElementException;
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

  private final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";
  private final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private MockService mockService;

  @Test
  void shouldReturnNotFoundWhenNoSuchElementException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(ExcerptNotFoundException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isNotFound())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof ExcerptNotFoundException))
        .andExpect(matchAll(
            jsonPath("$.code").value(is("NOT_FOUND")),
            jsonPath("$.details").doesNotExist()));
  }

  @Test
  void shouldReturnBadRequestWhenExcerptProcessingException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(new ExcerptProcessingException(FAILED, "test"));

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isBadRequest())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof ExcerptProcessingException))
        .andExpect(matchAll(
            jsonPath("$.status").value(is("FAILED")),
            jsonPath("$.statusDetails").value(is("test"))));
  }

  @Test
  void shouldReturnRuntimeErrorOnGenericException() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(RuntimeException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpect(matchAll(
            jsonPath("$.code").value(is("RUNTIME_ERROR")),
            jsonPath("$.statusDetails").doesNotExist()));
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
        .andExpect(matchAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is(METHOD_ARGUMENT_TYPE_MISMATCH)),
            jsonPath("$.details").doesNotExist())
        );
  }

  @Test
  void shouldReturn403WhenForbiddenOperation() throws Exception {
    when(mockService.getExcerpt(any())).thenThrow(AccessDeniedException.class);

    mockMvc
        .perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(
            matchAll(
                status().isForbidden(),
                jsonPath("$.code").value(is(FORBIDDEN_OPERATION))));
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
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    mockMvc.perform(post(BASE_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(inputStringBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(response ->
            assertTrue(response.getResolvedException() instanceof MethodArgumentNotValidException))
        .andExpect(content().json(expectedOutputBody));
  }

  public static FieldsValidationErrorDetails validationDetailsFrom(
      FieldsValidationErrorDetails.FieldError... details) {
    return new FieldsValidationErrorDetails(Arrays.asList(details));
  }
}
