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

package com.epam.digital.data.platform.excerpt.api.service;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.excerpt.api.exception.SigningNotAllowedException;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.Header;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.IN_PROGRESS;

@Service
public class ExcerptGenerationService {

  private static final Set<String> UNSIGNED_TYPES = Set.of("docx", "csv");
  
  private final RecordRepository recordRepository;
  private final TemplateRepository templateRepository;

  private final KafkaHelper kafkaHelper;
  private final JwtHelper jwtHelper;
  private final DigitalSignatureService digitalSignatureService;

  private final boolean isDigitalSignatureEnabled;

  public ExcerptGenerationService(
      RecordRepository recordRepository,
      TemplateRepository templateRepository,
      KafkaHelper kafkaHelper,
      JwtHelper jwtHelper,
      DigitalSignatureService digitalSignatureService,
      @Value("${dataplatform.signature.enabled}") boolean isDigitalSignatureEnabled) {
    this.recordRepository = recordRepository;
    this.templateRepository = templateRepository;
    this.kafkaHelper = kafkaHelper;
    this.jwtHelper = jwtHelper;
    this.digitalSignatureService = digitalSignatureService;
    this.isDigitalSignatureEnabled = isDigitalSignatureEnabled;
  }

  public ExcerptEntityId generateExcerpt(ExcerptEventDto excerptEventDto,
                                         RequestContext requestContext,
                                         SecurityContext securityContext) {
    var excerptType = excerptEventDto.getExcerptType();

    validateAndSaveSignatures(excerptEventDto, securityContext);
    var excerptTemplate = validateTemplate(excerptType);
    validateTemplateType(excerptTemplate, excerptEventDto);

    var newRecord = recordRepository.save(buildRecord(excerptEventDto, requestContext,
        securityContext, excerptTemplate.getTemplateType()));
    kafkaHelper.send(newRecord, excerptType, excerptEventDto.getExcerptInputData(), excerptTemplate.getTemplateType());

    return new ExcerptEntityId(newRecord.getId());
  }

  private void validateAndSaveSignatures(
      ExcerptEventDto excerptEventDto, SecurityContext securityContext) {
    if (!isDigitalSignatureEnabled) {
      return;
    }
    verifyMandatoryHeaders(securityContext);

    digitalSignatureService.checkSignature(
        excerptEventDto, securityContext.getDigitalSignatureDerived());

    digitalSignatureService.saveSignature(securityContext.getDigitalSignature());
    digitalSignatureService.saveSignature(securityContext.getDigitalSignatureDerived());
  }

  private void verifyMandatoryHeaders(SecurityContext context) {
    var missedHeaders = new ArrayList<String>();
    if (context.getDigitalSignature() == null) {
      missedHeaders.add(Header.X_DIGITAL_SIGNATURE.getHeaderName());
    }
    if (context.getDigitalSignatureDerived() == null) {
      missedHeaders.add(Header.X_DIGITAL_SIGNATURE_DERIVED.getHeaderName());
    }
    if (!missedHeaders.isEmpty()) {
      throw new MandatoryHeaderMissingException(missedHeaders);
    }
  }

  private ExcerptTemplate validateTemplate(String excerptType) {
    return templateRepository
        .findFirstByTemplateName(excerptType)
        .orElseThrow(
            () -> new ExcerptProcessingException(FAILED, "Template not found: " + excerptType));
  }
  
  private void validateTemplateType(ExcerptTemplate excerptTemplate, ExcerptEventDto eventDto) {
    String templateType = excerptTemplate.getTemplateType();
    if(UNSIGNED_TYPES.contains(templateType) && eventDto.isRequiresSystemSignature()) {
      throw new SigningNotAllowedException(templateType + " file not allowed to sign");
    }
  }

  private ExcerptRecord buildRecord(ExcerptEventDto excerptEventDto, RequestContext requestContext,
      SecurityContext securityContext, String templateType) {
    var excerptRecord = new ExcerptRecord();
    excerptRecord.setStatus(IN_PROGRESS);
    var now = LocalDateTime.now();
    excerptRecord.setCreatedAt(now);
    excerptRecord.setUpdatedAt(now);
    excerptRecord.setSignatureRequired(excerptEventDto.isRequiresSystemSignature());
    excerptRecord.setKeycloakId(jwtHelper.getKeycloakId(securityContext.getAccessToken()));

    excerptRecord.setxDigitalSignature(securityContext.getDigitalSignature());
    excerptRecord.setxDigitalSignatureDerived(securityContext.getDigitalSignatureDerived());
    excerptRecord.setxSourceSystem(requestContext.getSourceSystem());
    excerptRecord.setxSourceApplication(requestContext.getSourceApplication());
    excerptRecord.setxSourceBusinessProcess(requestContext.getSourceBusinessProcess());
    excerptRecord.setxSourceBusinessActivity(requestContext.getSourceBusinessActivity());
    excerptRecord.setExcerptType(templateType);
    return excerptRecord;
  }
}