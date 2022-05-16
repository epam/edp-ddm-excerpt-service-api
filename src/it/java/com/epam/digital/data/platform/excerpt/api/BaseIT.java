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

import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ComponentScan
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092",
    "port=9092"})
public abstract class BaseIT {

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected RecordRepository recordRepository;
  @Autowired
  protected TemplateRepository templateRepository;
  @Autowired
  protected ObjectMapper objectMapper;

  @AfterEach
  void cleanUp() {
    recordRepository.deleteAll();
    templateRepository.deleteAll();
  }

  protected ExcerptTemplate saveExcerptTemplateToDatabase(String name, String template) {
    var excerptTemplate = new ExcerptTemplate();
    excerptTemplate.setTemplateName(name);
    excerptTemplate.setTemplate(template);
    excerptTemplate.setTemplateType("pdf");
    return templateRepository.save(excerptTemplate);
  }

  protected ExcerptRecord saveExcerptRecordToDatabase(ExcerptRecord excerptRecord) {
    return recordRepository.save(excerptRecord);
  }
}
