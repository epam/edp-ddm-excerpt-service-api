package com.epam.digital.data.platform.excerpt.api.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.IN_PROGRESS;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.model.StatusDto;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class ExcerptService {

  private final RecordRepository recordRepository;
  private final TemplateRepository templateRepository;

  private final KafkaHelper kafkaHelper;
  private final JwtHelper jwtHelper;

  private final CephService excerptCephService;
  private final String bucket;

  public ExcerptService(
      RecordRepository recordRepository,
      TemplateRepository templateRepository,
      KafkaHelper kafkaHelper,
      JwtHelper jwtHelper,
      CephService excerptCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket) {
    this.recordRepository = recordRepository;
    this.templateRepository = templateRepository;
    this.kafkaHelper = kafkaHelper;
    this.jwtHelper = jwtHelper;
    this.excerptCephService = excerptCephService;
    this.bucket = bucket;
  }

  public ExcerptEntityId generateExcerpt(ExcerptEventDto excerptEventDto, SecurityContext context) {
    var excerptType = excerptEventDto.getExcerptType();

    validateTemplate(excerptType);

    var newRecord = recordRepository.save(buildRecord(excerptEventDto, context));
    kafkaHelper.send(newRecord, excerptType, excerptEventDto.getExcerptInputData());

    return new ExcerptEntityId(newRecord.getId());
  }

  private void validateTemplate(String excerptType) {
    templateRepository.findFirstByTemplateName(excerptType)
        .orElseThrow(
            () -> new ExcerptProcessingException(FAILED, "Template not found: " + excerptType));
  }

  public ByteArrayResource getExcerpt(UUID id, SecurityContext context) {
    byte[] cephValue;
    var excerpt = recordRepository.findById(id)
        .orElseThrow(() -> new ExcerptNotFoundException("Record not found in DB: " + id));

    validateKeycloakId(excerpt, context);

    cephValue =
        excerptCephService
            .getObject(bucket, excerpt.getExcerptKey())
            .orElseThrow(
                () ->
                    new ExcerptNotFoundException(
                        "Excerpt not found in Ceph: " + excerpt.getExcerptKey()))
            .getContent();

    return new ByteArrayResource(cephValue);
  }

  public StatusDto getStatus(UUID id) {
    var excerptRecord = recordRepository.findById(id)
        .orElseThrow(() -> new ExcerptNotFoundException("Record " + id + " not found"));

    return new StatusDto(excerptRecord.getStatus(), excerptRecord.getStatusDetails());
  }

  private void validateKeycloakId(ExcerptRecord excerpt, SecurityContext context) {
    var requestKeycloakId = jwtHelper.getKeycloakId(context.getAccessToken());

    if (!excerpt.getKeycloakId().equals(requestKeycloakId)) {
      throw new InvalidKeycloakIdException("KeycloakId does not match one stored in database");
    }
  }

  private ExcerptRecord buildRecord(ExcerptEventDto excerptEventDto, SecurityContext context) {
    var excerptRecord = new ExcerptRecord();
    excerptRecord.setStatus(IN_PROGRESS);
    var now = LocalDateTime.now();
    excerptRecord.setCreatedAt(now);
    excerptRecord.setUpdatedAt(now);
    excerptRecord.setSignatureRequired(excerptEventDto.isRequiresSystemSignature());
    excerptRecord.setKeycloakId(jwtHelper.getKeycloakId(context.getAccessToken()));
    return excerptRecord;
  }
}
