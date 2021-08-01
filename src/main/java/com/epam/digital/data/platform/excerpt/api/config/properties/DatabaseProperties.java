package com.epam.digital.data.platform.excerpt.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("data-platform.datasource")
public class DatabaseProperties {

  private static final long DEFAULT_TIMEOUT_IN_MILLIS = 30000;

  private String url;
  private String username;
  private String password;
  private long connectionTimeout = DEFAULT_TIMEOUT_IN_MILLIS;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
}
