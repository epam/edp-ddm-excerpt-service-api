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

package com.epam.digital.data.platform.excerpt.api.controller;

import com.epam.digital.data.platform.excerpt.api.annotation.HttpRequestContext;
import com.epam.digital.data.platform.excerpt.api.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.excerpt.api.audit.AuditableController;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptGenerationService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptRetrievingService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptStatusCheckService;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.StatusDto;
import java.util.UUID;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/excerpts")
public class ExcerptController {

  private static final String CONTENT_DISPOSITION_HEADER_NAME = "Content-Disposition";
  private static final String ATTACHMENT_HEADER_VALUE = "attachment; filename=\"%s.%s\"";

  private final Logger log = LoggerFactory.getLogger(ExcerptController.class);

  private final ExcerptGenerationService excerptGenerationService;
  private final ExcerptRetrievingService excerptRetrievingService;
  private final ExcerptStatusCheckService excerptStatusCheckService;

  public ExcerptController(
      ExcerptGenerationService excerptGenerationService,
      ExcerptRetrievingService excerptRetrievingService,
      ExcerptStatusCheckService excerptStatusCheckService) {
    this.excerptGenerationService = excerptGenerationService;
    this.excerptRetrievingService = excerptRetrievingService;
    this.excerptStatusCheckService = excerptStatusCheckService;
  }

  @AuditableController(action = "GENERATE EXCERPT CALL")
  @PostMapping
  public ResponseEntity<ExcerptEntityId> generate(
      @Valid @RequestBody ExcerptEventDto excerptEventDto,
      @HttpRequestContext RequestContext requestContext,
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("Excerpt generation called");
    return ResponseEntity.ok()
        .body(excerptGenerationService.generateExcerpt(excerptEventDto, requestContext, securityContext));
  }

  @AuditableController(action = "RETRIEVE EXCERPT CALL")
  @GetMapping("/{id}")
  public ResponseEntity<Resource> retrieve(
      @PathVariable("id") UUID id, @HttpSecurityContext SecurityContext securityContext) {
    log.info("Excerpt retrieval called");

    var excerpt = excerptRetrievingService.getExcerpt(id, securityContext);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(excerpt.getCephObject().getMetadata().getContentLength())
        .header(CONTENT_DISPOSITION_HEADER_NAME,
            String.format(ATTACHMENT_HEADER_VALUE, id.toString(), excerpt.getExcerptType()))
        .body(new InputStreamResource(excerpt.getCephObject().getContent()));
  }

  @GetMapping("/{id}/status")
  public ResponseEntity<StatusDto> status(@PathVariable("id") UUID id) {
    log.info("Excerpt status retrieval called");

    var status = excerptStatusCheckService.getStatus(id);
    return ResponseEntity.ok().body(status);
  }
}
