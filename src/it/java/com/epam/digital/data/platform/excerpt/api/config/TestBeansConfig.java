/*
 * Copyright 2023 EPAM Systems.
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


import com.epam.digital.data.platform.excerpt.api.service.ExcerptGenerationService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptRetrievingService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptStatusCheckService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestBeansConfig {

  @Bean
  public ExcerptGenerationService testExcerptGenerationService() {
    return Mockito.mock(ExcerptGenerationService.class);
  }

  @Bean
  public ExcerptRetrievingService testExcerptRetrievingService() {
    return Mockito.mock(ExcerptRetrievingService.class);
  }
  @Bean
  public ExcerptStatusCheckService testExcerptStatusCheckService() {
    return Mockito.mock(ExcerptStatusCheckService.class);
  }
}
