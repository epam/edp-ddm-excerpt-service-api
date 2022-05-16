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

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.Request;
import java.util.Map;

import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
      KafkaTemplate<String, Request<ExcerptEventDto>> kafkaTemplate,
      KafkaProperties kafkaProperties,
      RecordRepository recordRepository) {
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties = kafkaProperties;
    this.recordRepository = recordRepository;
  }

  public void send(ExcerptRecord newRecord, String name, Map<String, Object> json, String templateType) {
    var event = new ExcerptEventDto(newRecord.getId(), name, json, newRecord.getSignatureRequired());
    Request<ExcerptEventDto> request = new Request<>(event);

    log.info("Send Excerpt generation Event to Kafka");
    var topic = kafkaProperties.getTopics().get(templateType);
    var future = kafkaTemplate.send(topic, request);
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
