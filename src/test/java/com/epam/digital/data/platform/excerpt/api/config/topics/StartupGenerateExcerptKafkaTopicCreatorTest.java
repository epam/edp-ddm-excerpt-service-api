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

import static com.epam.digital.data.platform.starter.actuator.readinessprobe.KafkaConstants.KAFKA_HEALTH_TOPIC;
import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties;
import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties.ErrorHandler;
import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties.TopicProperties;
import com.epam.digital.data.platform.excerpt.api.exception.CreateKafkaTopicException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartupGenerateExcerptKafkaTopicCreatorTest {

  private static final String GENERATE_EXCERPT_TOPIC = "generate-excerpt";
  private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000L;

  private static final int RETENTION_IN_DAYS = 3;
  private static final short REPLICATION_FACTOR = 2;
  private static final int NUM_PARTITIONS = 1;


  private Set<String> existedTopics;
  private StartupGenerateExcerptKafkaTopicCreator instance;

  @Mock
  private AdminClient adminClient;
  @Mock
  private KafkaFuture<Void> createTopicsFuture;
  @Mock
  private KafkaFuture<Set<String>> listTopicsFuture;
  @Mock
  private CreateTopicsResult createTopicsResult;
  @Mock
  private ListTopicsResult listTopicsResult;
  @Captor
  private ArgumentCaptor<Set<NewTopic>> setArgumentCaptor;

  @BeforeEach
  void setup() {
    instance = new StartupGenerateExcerptKafkaTopicCreator(() -> adminClient, mockKafkaProperties());
    existedTopics = new HashSet<>();
    existedTopics.add("some-topic");
    existedTopics.add("another-topic");
  }

  @Test
  void shouldCreateTopic() throws Exception {
    customizeAdminClientMock(existedTopics);
    when(createTopicsResult.values())
            .thenReturn(Collections.singletonMap(GENERATE_EXCERPT_TOPIC, createTopicsFuture));
    when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);

    instance.createKafkaTopic();

    verify(adminClient).createTopics(setArgumentCaptor.capture());
    var resultSet = setArgumentCaptor.getValue();

    assertThat(resultSet.size()).isEqualTo(1);

    var newTopic = resultSet.stream().findFirst().get();

    assertThat(newTopic.name()).isEqualTo(GENERATE_EXCERPT_TOPIC);
    assertThat(newTopic.configs()).hasSize(1);
    assertThat(newTopic.numPartitions()).isEqualTo(NUM_PARTITIONS);
    assertThat(newTopic.replicationFactor()).isEqualTo(REPLICATION_FACTOR);
    assertThat(Long.valueOf(newTopic.configs().get(RETENTION_MS_CONFIG)))
        .isEqualTo(RETENTION_IN_DAYS * DAYS_TO_MS);
  }

  @Test
  void shouldNotCreateTopic() throws Exception {
    customizeAdminClientMock(existedTopics);
    existedTopics.add(GENERATE_EXCERPT_TOPIC);

    instance.createKafkaTopic();

    verify(adminClient, never()).createTopics(setArgumentCaptor.capture());
  }

  @Test
  void shouldThrowExceptionWhenCannotConnectToKafka() {
    when(adminClient.listTopics()).thenThrow(new CreateKafkaTopicException("any", null));

    assertThatThrownBy(() -> instance.createKafkaTopic())
        .isInstanceOf(CreateKafkaTopicException.class);
  }

  @Test
  void shouldThrowExceptionWhenNonSuccessTopicCreation() throws Exception {
    customizeAdminClientMock(existedTopics);
    when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
    when(createTopicsResult.values())
            .thenReturn(Collections.singletonMap(KAFKA_HEALTH_TOPIC, createTopicsFuture));
    when(createTopicsFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(new RuntimeException());


    assertThatThrownBy(() -> instance.createKafkaTopic())
        .isInstanceOf(CreateKafkaTopicException.class);
  }

  @Test
  void shouldNotFailIfTopicExistsException() throws Exception {
    customizeAdminClientMock(existedTopics);
    when(createTopicsResult.values())
            .thenReturn(Collections.singletonMap(KAFKA_HEALTH_TOPIC, createTopicsFuture));
    when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
    when(createTopicsFuture.get(anyLong(), any(TimeUnit.class)))
            .thenThrow(new RuntimeException(new TopicExistsException("")));

    instance.createKafkaTopic();
  }

  private void customizeAdminClientMock(Set<String> topics) throws Exception {
    when(adminClient.listTopics()).thenReturn(listTopicsResult);
    when(listTopicsResult.names()).thenReturn(listTopicsFuture);
    doReturn(topics).when(listTopicsFuture).get(anyLong(), any(TimeUnit.class));
  }

  private KafkaProperties mockKafkaProperties() {
    var errorHandler = new ErrorHandler();
    errorHandler.setMaxElapsedTime(5000L);

    var topicProperties = new TopicProperties();
    topicProperties.setNumPartitions(NUM_PARTITIONS);
    topicProperties.setReplicationFactor(REPLICATION_FACTOR);
    topicProperties.setRetentionPolicyInDays(RETENTION_IN_DAYS);

    var kafkaProperties = new KafkaProperties();
    kafkaProperties.setTopic(GENERATE_EXCERPT_TOPIC);
    kafkaProperties.setErrorHandler(errorHandler);
    kafkaProperties.setTopicProperties(topicProperties);

    return kafkaProperties;
  }
}
