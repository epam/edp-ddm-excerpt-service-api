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
  private static final Long TOPIC_CREATION_TIMEOUT = 60L;

  private final AdminClient kafkaAdminClient;
  private final KafkaProperties kafkaProperties;

  public StartupGenerateExcerptKafkaTopicCreator(AdminClient kafkaAdminClient,
      KafkaProperties kafkaProperties) {
    this.kafkaAdminClient = kafkaAdminClient;
    this.kafkaProperties = kafkaProperties;
  }

  @PostConstruct
  public void createKafkaTopic() {
    if (!isTopicExist()) {
      create();
    }
  }

  private boolean isTopicExist() {
    boolean isExist;
    try {
      isExist = kafkaAdminClient.listTopics()
          .names()
          .get(TOPIC_CREATION_TIMEOUT, TimeUnit.SECONDS)
          .contains(kafkaProperties.getTopic());
    } catch (Exception e) {
      throw new CreateKafkaTopicException(String.format(
          "Failed to retrieve existing kafka topics in %d sec", TOPIC_CREATION_TIMEOUT), e);
    }
    return isExist;
  }

  private void create() {
    var createTopicsResult = kafkaAdminClient.createTopics(customize(kafkaProperties.getTopic()));
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
