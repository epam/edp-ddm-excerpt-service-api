package com.epam.digital.data.platform.excerpt.api.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("data-platform.kafka")
public class KafkaProperties {

  private String bootstrap;
  private String groupId;
  private List<String> trustedPackages;
  private TopicProperties topicProperties;
  private ErrorHandler errorHandler = new ErrorHandler();
  private String topic;

  public List<String> getTrustedPackages() {
    return trustedPackages;
  }

  public void setTrustedPackages(List<String> trustedPackages) {
    this.trustedPackages = trustedPackages;
  }

  public String getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(String bootstrap) {
    this.bootstrap = bootstrap;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public TopicProperties getTopicProperties() {
    return topicProperties;
  }

  public void setTopicProperties(
      TopicProperties topicProperties) {
    this.topicProperties = topicProperties;
  }

  public static class ErrorHandler {

    private Long initialInterval;
    private Long maxElapsedTime;
    private Double multiplier;

    public Long getInitialInterval() {
      return initialInterval;
    }

    public void setInitialInterval(Long initialInterval) {
      this.initialInterval = initialInterval;
    }

    public Long getMaxElapsedTime() {
      return maxElapsedTime;
    }

    public void setMaxElapsedTime(Long maxElapsedTime) {
      this.maxElapsedTime = maxElapsedTime;
    }

    public Double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(Double multiplier) {
      this.multiplier = multiplier;
    }
  }

  public static class TopicProperties {

    private Integer numPartitions;
    private Short replicationFactor;
    private Integer retentionPolicyInDays;

    public Integer getNumPartitions() {
      return numPartitions;
    }

    public void setNumPartitions(Integer numPartitions) {
      this.numPartitions = numPartitions;
    }

    public Short getReplicationFactor() {
      return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
      this.replicationFactor = replicationFactor;
    }

    public Integer getRetentionPolicyInDays() {
      return retentionPolicyInDays;
    }

    public void setRetentionPolicyInDays(Integer retentionPolicyInDays) {
      this.retentionPolicyInDays = retentionPolicyInDays;
    }
  }
}

