package com.epam.digital.data.platform.excerpt.api.config.topics;

import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

import com.epam.digital.data.platform.excerpt.api.config.properties.KafkaProperties;
import com.epam.digital.data.platform.excerpt.api.exception.CreateKafkaTopicException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

@Component
public class StartupHealthCheckKafkaTopicCreator {

  private static final String KAFKA_HEALTH_TOPIC = "kafka-health-check";
  private static final Long RETENTION_MS = 1000L;

  private static final int NUM_PARTITIONS = 1;
  private static final short REPLICATION_FACTOR = 1;

  private final AdminClient adminClient;
  private final KafkaProperties kafkaProperties;

  public StartupHealthCheckKafkaTopicCreator(AdminClient adminClient,
      KafkaProperties kafkaProperties) {
    this.adminClient = adminClient;
    this.kafkaProperties = kafkaProperties;
  }

  @PostConstruct
  public void createKafkaTopic() {
    long maxElapsedTime = kafkaProperties.getErrorHandler().getMaxElapsedTime();
    if (!isTopicExist(maxElapsedTime)) {
      create(maxElapsedTime);
    }
  }

  private boolean isTopicExist(long maxElapsedTime) {
    boolean isExist;
    try {
      isExist = adminClient.listTopics()
          .names()
          .get(maxElapsedTime, TimeUnit.MILLISECONDS)
          .contains(KAFKA_HEALTH_TOPIC);
    } catch (Exception e) {
      throw new CreateKafkaTopicException("Failed to retrieve existing kafka topics: ", e);
    }
    return isExist;
  }

  private void create(long maxElapsedTime) {
    var createTopicsResult = adminClient.createTopics(customize(KAFKA_HEALTH_TOPIC));
    try {
      createTopicsResult.all().get(maxElapsedTime, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      throw new CreateKafkaTopicException("Failed to create a kafka topic: ", e);
    }
  }

  private Collection<NewTopic> customize(String topicName) {
    var newTopic = new NewTopic(topicName, NUM_PARTITIONS, REPLICATION_FACTOR);
    newTopic.configs(Map.of(RETENTION_MS_CONFIG, Long.toString(RETENTION_MS)));
    return Set.of(newTopic);
  }
}
