package com.epam.digital.data.platform.excerpt.api.exception;

public class CreateKafkaTopicException extends RuntimeException {

  public CreateKafkaTopicException(String message, Exception e) {
    super(message, e);
  }
}
