package com.epam.digital.data.platform.excerpt.api.config;

import com.epam.digital.data.platform.storage.form.config.RedisStorageConfiguration;
import com.epam.digital.data.platform.storage.form.factory.StorageServiceFactory;
import com.epam.digital.data.platform.storage.form.service.FormDataStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class RedisFormDataStorageConfig {

  @Bean
  @ConditionalOnProperty(prefix = "storage.lowcode-form-form-data-storage", name = "type", havingValue = "redis")
  @ConfigurationProperties(prefix = "storage.lowcode-form-form-data-storage.backend.redis")
  public RedisStorageConfiguration lowcodeRedisFormDataStorageConfiguration() {
    return new RedisStorageConfiguration();
  }

  @Bean
  @ConditionalOnBean(name = "lowcodeRedisFormDataStorageConfiguration")
  public FormDataStorageService lowcodeFormDataStorageService(StorageServiceFactory factory,
      RedisConnectionFactory redisConnectionFactory) {
    return factory.formDataStorageService(redisConnectionFactory);
  }

  @Bean
  @ConditionalOnBean(name = "lowcodeRedisFormDataStorageConfiguration")
  public RedisConnectionFactory redisConnectionFactory(StorageServiceFactory factory,
      RedisStorageConfiguration lowcodeRedisFormDataStorageConfiguration) {
    return factory.redisConnectionFactory(lowcodeRedisFormDataStorageConfiguration);
  }
}
