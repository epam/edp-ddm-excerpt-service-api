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

package com.epam.digital.data.platform.excerpt.api.config;

import com.epam.digital.data.platform.storage.form.config.CephStorageConfiguration;
import com.epam.digital.data.platform.storage.form.factory.StorageServiceFactory;
import com.epam.digital.data.platform.storage.form.service.FormDataStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CephFormDataStorageConfig {

  @Bean
  @ConditionalOnProperty(prefix = "storage.lowcode-form-form-data-storage", name = "type", havingValue = "ceph")
  @ConfigurationProperties(prefix = "storage.lowcode-form-form-data-storage.backend.ceph")
  public CephStorageConfiguration lowcodeCephFormDataStorageConfiguration() {
    return new CephStorageConfiguration();
  }

  @Bean
  @ConditionalOnProperty(prefix = "storage.datafactory-excerpt-signature-storage", name = "type", havingValue = "ceph")
  @ConfigurationProperties(prefix = "storage.datafactory-excerpt-signature-storage.backend.ceph")
  public CephStorageConfiguration datafactoryExcerptDataStorageConfiguration() {
    return new CephStorageConfiguration();
  }

  @Bean
  @ConditionalOnBean(name = "lowcodeCephFormDataStorageConfiguration")
  public FormDataStorageService lowcodeFormDataStorageService(StorageServiceFactory factory,
      CephStorageConfiguration lowcodeCephFormDataStorageConfiguration) {
    return factory.formDataStorageService(lowcodeCephFormDataStorageConfiguration);
  }

  @Bean
  @ConditionalOnBean(name = "datafactoryExcerptDataStorageConfiguration")
  public FormDataStorageService datafactoryExcerptDataStorageService(StorageServiceFactory factory,
      CephStorageConfiguration datafactoryExcerptDataStorageConfiguration) {
    return factory.formDataStorageService(datafactoryExcerptDataStorageConfiguration);
  }
}
