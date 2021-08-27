package com.epam.digital.data.platform.excerpt.api.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.Request;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class KafkaHelper {

  private final Logger log = LoggerFactory.getLogger(KafkaHelper.class);

  private final KafkaTemplate<String, Request<ExcerptEventDto>> kafkaTemplate;
  private final KafkaProperties kafkaProperties;
  private final RecordRepository recordRepository;

  public KafkaHelper(
      @Qualifier("excerptKafka") KafkaTemplate<String, Request<ExcerptEventDto>> kafkaTemplate,
      KafkaProperties kafkaProperties,
      RecordRepository recordRepository) {
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties = kafkaProperties;
    this.recordRepository = recordRepository;
  }

  public void send(ExcerptRecord newRecord, String name, Map<String, Object> json) {
    var event = new ExcerptEventDto(newRecord.getId(), name, json, newRecord.getSignatureRequired());
    Request<ExcerptEventDto> request = new Request<>(event);

    log.info("Send Excerpt generation Event to Kafka");
    var future = kafkaTemplate.send(kafkaProperties.getTopic(), request);
    future.addCallback(new ListenableFutureCallback<>() {

      @Override
      public void onSuccess(SendResult<String, Request<ExcerptEventDto>> result) {

      }

      @Override
      public void onFailure(Throwable ex) {
        newRecord.setStatus(FAILED);
        newRecord.setStatusDetails("Failed to send data for processing");
        recordRepository.save(newRecord);
      }
    });
  }
}
