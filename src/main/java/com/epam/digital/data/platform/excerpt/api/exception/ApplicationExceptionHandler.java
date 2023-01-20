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

import static com.epam.digital.data.platform.excerpt.api.util.Header.TRACE_ID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.epam.digital.data.platform.dso.client.exception.InvalidSignatureException;
import com.epam.digital.data.platform.excerpt.api.audit.AuditableException;
import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.excerpt.model.StatusDto;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

  private static final String NOT_FOUND = "NOT_FOUND";
  private static final String THIRD_PARTY_SERVICE_UNAVAILABLE = "THIRD_PARTY_SERVICE_UNAVAILABLE";
  private static final String INTERNAL_CONTRACT_VIOLATION = "INTERNAL_CONTRACT_VIOLATION";
  private static final String SIGNATURE_VIOLATION = "SIGNATURE_VIOLATION";
  private static final String INVALID_HEADER_VALUE = "INVALID_HEADER_VALUE";
  private static final String HEADERS_ARE_MISSING = "HEADERS_ARE_MISSING";
  private static final String INVALID_KEYCLOAK_ID = "INVALID_KEYCLOAK_ID";
  private static final String RUNTIME_ERROR = "RUNTIME_ERROR";
  private static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  private static final String CLIENT_ERROR = "CLIENT_ERROR";
  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  private static final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";
  private static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
  private static final String SIGNING_NOT_ALLOWED = "SIGNING_NOT_ALLOWED";

  @AuditableException
  @ExceptionHandler(CephCommunicationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleCephCommunicationException(
      Exception exception) {
    log.error("Exception while communication with ceph", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(THIRD_PARTY_SERVICE_UNAVAILABLE));
  }

  @AuditableException
  @ExceptionHandler(MisconfigurationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMisconfigurationException(
      Exception exception) {
    log.error("Ceph bucket not found", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(INTERNAL_CONTRACT_VIOLATION));
  }

  @AuditableException
  @ExceptionHandler(KepServiceInternalServerErrorException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleInternalServerErrorException(
      Exception exception) {
    log.error("External digital signature service has internal server error", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(THIRD_PARTY_SERVICE_UNAVAILABLE));
  }

  @AuditableException
  @ExceptionHandler(KepServiceBadRequestException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleBadRequestException(
      Exception exception) {
    log.error("Call to external digital signature service violates an internal contract",
        exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(INTERNAL_CONTRACT_VIOLATION));
  }

  @AuditableException
  @ExceptionHandler(InvalidSignatureException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleSignatureValidationException(
      InvalidSignatureException exception) {
    log.error("Digital signature validation failed", exception);
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .body(newDetailedResponse(SIGNATURE_VIOLATION));
  }

  @AuditableException
  @ExceptionHandler(DigitalSignatureNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleCephNoSuchObjectException(
      DigitalSignatureNotFoundException exception) {
    log.error("Digital signature not found", exception);
    DetailedErrorResponse<Void> responseBody =
        newDetailedResponse(INVALID_HEADER_VALUE);
    return ResponseEntity.status(BAD_REQUEST).body(responseBody);
  }
  
  @AuditableException
  @ExceptionHandler(SigningNotAllowedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleSigningNotAllowedException(
      SigningNotAllowedException exception) {
    log.error("File of this type is not allowed to be signed", exception);
    DetailedErrorResponse<Void> responseBody =
        newDetailedResponse(SIGNING_NOT_ALLOWED);
    return ResponseEntity.status(BAD_REQUEST).body(responseBody);
  }

  @AuditableException
  @ExceptionHandler(MandatoryHeaderMissingException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMandatoryHeaderMissingException(
      Exception exception) {
    log.error("Mandatory header(s) missed", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(HEADERS_ARE_MISSING));
  }

  @AuditableException
  @ExceptionHandler(ExcerptProcessingException.class)
  public ResponseEntity<StatusDto> handleTemplateNotFoundException(
      ExcerptProcessingException exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new StatusDto(exception.getStatus(), exception.getMessage()));
  }

  @AuditableException
  @ExceptionHandler(ExcerptNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleRecordNotFoundException(
      ExcerptNotFoundException exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(NOT_FOUND));
  }

  @AuditableException
  @ExceptionHandler(InvalidKeycloakIdException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleInvalidKeycloakIdException(
      InvalidKeycloakIdException exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(INVALID_KEYCLOAK_ID));
  }

  @AuditableException
  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR));
  }

  @AuditableException(userInfoEnabled = false)
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAuthenticationException(
          AuthenticationException exception) {
    log.error("Authentication failure", exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(newDetailedResponse(AUTHENTICATION_FAILED));
  }

  @AuditableException
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(FORBIDDEN_OPERATION));
  }

  @AuditableException
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMethodArgumentTypeMismatchException(
      Exception exception) {
    log.error("Path argument is not valid", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(METHOD_ARGUMENT_TYPE_MISMATCH));
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("One or more input arguments are not valid", exception);
    DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
        = newDetailedResponse(VALIDATION_ERROR);

    var generalErrorList = exception.getBindingResult().getFieldErrors();
    var customErrorsDetails = generalErrorList.stream()
        .map(error -> new FieldsValidationErrorDetails.FieldError(error.getRejectedValue(),
            error.getField(), error.getDefaultMessage()))
        .collect(toList());
    invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(customErrorsDetails));

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(invalidFieldsResponse);
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    var parseException = handleParseException(exception);
    if (parseException.isPresent()) {
      log.error("Can not read some of arguments", exception);
      var response = newDetailedResponse(CLIENT_ERROR);
      response.setDetails(parseException.get());
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(response);
    }

    log.error("Request body is not readable JSON", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(CLIENT_ERROR));
  }

  private Optional<DetailedErrorResponse<FieldsValidationErrorDetails>> handleParseException(
      HttpMessageNotReadableException exception) {
    if (exception.getCause() instanceof InvalidFormatException) {
      var ex = (InvalidFormatException) exception.getCause();

      var msg = ex.getOriginalMessage();
      if (ex.getCause() instanceof DateTimeParseException) {
        msg = ex.getCause().getMessage();
      }

      var value = String.valueOf(ex.getValue());

      var field = ex.getPath().stream()
          .map(Reference::getFieldName)
          .collect(joining("."));

      DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
          = newDetailedResponse(VALIDATION_ERROR);

      var details = List.of(new FieldsValidationErrorDetails.FieldError(value, field, msg));
      invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(details));

      return Optional.of(invalidFieldsResponse);
    }

    return Optional.empty();
  }

  private <T> DetailedErrorResponse<T> newDetailedResponse(String code) {
    var response = new DetailedErrorResponse<T>();
    response.setTraceId(MDC.get(TRACE_ID.getHeaderName()));
    response.setCode(code);
    return response;
  }
}
