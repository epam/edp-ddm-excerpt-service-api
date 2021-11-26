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

package com.epam.digital.data.platform.excerpt.api;

import com.epam.digital.data.platform.excerpt.api.annotation.HttpRequestContext;
import com.epam.digital.data.platform.excerpt.api.annotation.HttpSecurityContext;
import io.swagger.v3.core.util.PrimitiveType;
import org.springdoc.core.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.epam.digital.data.platform.excerpt.dao")
@SpringBootApplication
public class ExcerptServiceApiApplication {

  static {
    SpringDocUtils.getConfig()
        .addAnnotationsToIgnore(HttpSecurityContext.class, HttpRequestContext.class);
    PrimitiveType.enablePartialTime();
  }

  public static void main(String[] args) {
    SpringApplication.run(ExcerptServiceApiApplication.class, args);
  }
}
