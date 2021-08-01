package com.epam.digital.data.platform.excerpt.api.config;

import com.epam.digital.data.platform.excerpt.api.config.properties.DatabaseProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DbConfiguration {

  @Bean
  public DataSource datasource(DatabaseProperties databaseProperties) {
    HikariConfig configuration = new HikariConfig();
    configuration.setJdbcUrl(databaseProperties.getUrl());
    configuration.setUsername(databaseProperties.getUsername());
    configuration.setPassword(databaseProperties.getPassword());
    configuration.setConnectionTimeout(databaseProperties.getConnectionTimeout());
    return new HikariDataSource(configuration);
  }
}
