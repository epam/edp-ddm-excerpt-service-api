package com.epam.digital.data.platform.excerpt.api.exception;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.excerpt.api.model.StatusDto;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

  private final String NOT_FOUND = "NOT_FOUND";
  private final String RUNTIME_ERROR = "RUNTIME_ERROR";
  private final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  private final String CLIENT_ERROR = "CLIENT_ERROR";
  private final String VALIDATION_ERROR = "VALIDATION_ERROR";
  private final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";

  @ExceptionHandler(ExcerptProcessingException.class)
  public ResponseEntity<StatusDto> handleTemplateNotFoundException(
      ExcerptProcessingException exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new StatusDto(exception.getStatus(), exception.getMessage()));
  }

  @ExceptionHandler(ExcerptNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleRecordNotFoundException(
      ExcerptNotFoundException exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(NOT_FOUND));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(FORBIDDEN_OPERATION));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMethodArgumentTypeMismatchException(
      Exception exception) {
    log.error("Path argument is not valid", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(METHOD_ARGUMENT_TYPE_MISMATCH));
  }

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

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    var parseException = handleParseException(exception);
    if (parseException.isPresent()) {
      log.error("Can not read some of arguments", exception);
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(parseException.get());
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
    response.setCode(code);
    return response;
  }
}
