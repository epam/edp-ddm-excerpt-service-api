package com.epam.digital.data.platform.excerpt.api.config.topics;

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
import com.epam.digital.data.platform.excerpt.api.exception.CreateKafkaTopicException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartupHealthCheckKafkaTopicCreatorTest {

  private static final String KAFKA_HEALTH_TOPIC = "kafka-health-check";
  private static final String RETENTION_MS = Long.toString(1000L);

  private Set<String> existedTopics;
  private StartupHealthCheckKafkaTopicCreator instance;

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
    instance = new StartupHealthCheckKafkaTopicCreator(adminClient, mockKafkaProperties());
    existedTopics = new HashSet<>();
    existedTopics.add("some-topic");
    existedTopics.add("another-topic");
  }

  @Test
  void shouldCreateTopic() throws Exception {
    customizeAdminClientMock(existedTopics);
    when(createTopicsResult.all()).thenReturn(createTopicsFuture);
    when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);

    instance.createKafkaTopic();

    verify(adminClient).createTopics(setArgumentCaptor.capture());
    var resultSet = setArgumentCaptor.getValue();

    assertThat(resultSet.size()).isEqualTo(1);

    var newTopic = resultSet.stream().findFirst().get();

    assertThat(newTopic.name()).isEqualTo(KAFKA_HEALTH_TOPIC);
    assertThat(newTopic.configs().size()).isEqualTo(1);
    assertThat(newTopic.configs().get(RETENTION_MS_CONFIG)).isEqualTo(RETENTION_MS);
  }

  @Test
  void shouldNotCreateTopic() throws Exception {
    customizeAdminClientMock(existedTopics);
    existedTopics.add(KAFKA_HEALTH_TOPIC);

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
  void shouldThrowExceptionWhenTimeExceededLimit() throws Exception {
    customizeAdminClientMock(existedTopics);

    assertThatThrownBy(() -> instance.createKafkaTopic())
        .isInstanceOf(CreateKafkaTopicException.class);
  }

  private void customizeAdminClientMock(Set<String> topics) throws Exception {
    when(adminClient.listTopics()).thenReturn(listTopicsResult);
    when(listTopicsResult.names()).thenReturn(listTopicsFuture);
    doReturn(topics).when(listTopicsFuture).get(anyLong(), any(TimeUnit.class));
  }

  private KafkaProperties mockKafkaProperties() {
    var errorHandler = new ErrorHandler();
    errorHandler.setMaxElapsedTime(5000L);

    var kafkaProperties = new KafkaProperties();
    kafkaProperties.setErrorHandler(errorHandler);

    return kafkaProperties;
  }
}
