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
public class StartupGenerateExcerptKafkaTopicCreator {
  private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000L;

  private final AdminClient kafkaAdminClient;
  private final KafkaProperties kafkaProperties;

  public StartupGenerateExcerptKafkaTopicCreator(AdminClient kafkaAdminClient,
      KafkaProperties kafkaProperties) {
    this.kafkaAdminClient = kafkaAdminClient;
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
      isExist = kafkaAdminClient.listTopics()
          .names()
          .get(maxElapsedTime, TimeUnit.MILLISECONDS)
          .contains(kafkaProperties.getTopic());
    } catch (Exception e) {
      throw new CreateKafkaTopicException("Failed to retrieve existing kafka topics: ", e);
    }
    return isExist;
  }

  private void create(long maxElapsedTime) {
    var createTopicsResult = kafkaAdminClient.createTopics(customize(kafkaProperties.getTopic()));
    try {
      createTopicsResult.all().get(maxElapsedTime, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      throw new CreateKafkaTopicException("Failed to create a kafka topic: ", e);
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
