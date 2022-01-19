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

package com.epam.digital.data.platform.excerpt.api.config.topics;

import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties;
import com.epam.digital.data.platform.excerpt.api.exception.CreateKafkaTopicException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

@Component
public class StartupGenerateExcerptKafkaTopicCreator {

  private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000L;
  private static final Long TOPIC_CREATION_TIMEOUT = 60L;

  private final Supplier<AdminClient> adminClientFactory;
  private final KafkaProperties kafkaProperties;

  public StartupGenerateExcerptKafkaTopicCreator(Supplier<AdminClient> adminClientFactory,
      KafkaProperties kafkaProperties) {
    this.adminClientFactory = adminClientFactory;
    this.kafkaProperties = kafkaProperties;
  }

  @PostConstruct
  public void createKafkaTopic() {
    try (var adminClient = adminClientFactory.get()) {
      if (!isTopicExist(adminClient)) {
        create(adminClient);
      }
    }
  }

  private boolean isTopicExist(AdminClient adminClient) {
    boolean isExist;
    try {
      isExist = adminClient.listTopics()
          .names()
          .get(TOPIC_CREATION_TIMEOUT, TimeUnit.SECONDS)
          .contains(kafkaProperties.getTopic());
    } catch (Exception e) {
      throw new CreateKafkaTopicException(String.format(
          "Failed to retrieve existing kafka topics in %d sec", TOPIC_CREATION_TIMEOUT), e);
    }
    return isExist;
  }

  private void create(AdminClient adminClient) {
    var createTopicsResult = adminClient.createTopics(customize(kafkaProperties.getTopic()));
    try {
      createTopicsResult.all().get(TOPIC_CREATION_TIMEOUT, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new CreateKafkaTopicException(
          String.format("Failed to create the kafka topic '%s' in %d sec", kafkaProperties.getTopic(),
              TOPIC_CREATION_TIMEOUT), e);
    }
  }

  private Collection<NewTopic> customize(String topicName) {
    var newTopic = new NewTopic(topicName, kafkaProperties.getTopicProperties().getNumPartitions(),
        kafkaProperties.getTopicProperties().getReplicationFactor());
    var retentionInDays = kafkaProperties.getTopicProperties().getRetentionPolicyInDays();
    newTopic.configs(Map.of(RETENTION_MS_CONFIG, Long.toString(retentionInDays * DAYS_TO_MS)));
    return Set.of(newTopic);
  }
}
