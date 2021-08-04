package com.epam.digital.data.platform.excerpt.api.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.IN_PROGRESS;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.model.StatusDto;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class ExcerptService {

  private final RecordRepository recordRepository;
  private final TemplateRepository templateRepository;

  private final KafkaHelper kafkaHelper;

  private final CephService excerptCephService;
  private final String bucket;

  public ExcerptService(
      RecordRepository recordRepository,
      TemplateRepository templateRepository,
      KafkaHelper kafkaHelper, CephService excerptCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket) {
    this.recordRepository = recordRepository;
    this.templateRepository = templateRepository;
    this.kafkaHelper = kafkaHelper;
    this.excerptCephService = excerptCephService;
    this.bucket = bucket;
  }

  public ExcerptEntityId generateExcerpt(ExcerptEventDto excerptEventDto) {
    var excerptType = excerptEventDto.getExcerptType();

    validateTemplate(excerptType);

    var newRecord = recordRepository.save(buildRecord(excerptEventDto));
    kafkaHelper.send(newRecord, excerptType, excerptEventDto.getExcerptInputData());

    return new ExcerptEntityId(newRecord.getId());
  }

  private void validateTemplate(String excerptType) {
    templateRepository.findFirstByTemplateName(excerptType)
        .orElseThrow(() -> new ExcerptProcessingException(FAILED, "Template not found: " + excerptType));
  }

  public ByteArrayResource getExcerpt(UUID id) {
    String cephValue;
    var excerpt = recordRepository.findById(id)
        .orElseThrow(() -> new ExcerptNotFoundException("Record not found in DB: " + id));

    try {
      cephValue = excerptCephService.getContent(bucket, excerpt.getExcerptKey())
          .orElseThrow(() ->
              new ExcerptNotFoundException("Excerpt not found in Ceph: " + excerpt.getExcerptKey()));
    } catch (ExcerptNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ExcerptProcessingException(FAILED, e.getMessage());
    }

    return new ByteArrayResource(Base64.getDecoder().decode(cephValue));
  }

  public StatusDto getStatus(UUID id) {
    var excerptRecord = recordRepository.findById(id)
        .orElseThrow(() -> new ExcerptProcessingException(FAILED, "Record not found: " + id));

    return new StatusDto(excerptRecord.getStatus(), excerptRecord.getStatusDetails());
  }

  private ExcerptRecord buildRecord(ExcerptEventDto excerptEventDto) {
    var excerptRecord = new ExcerptRecord();
    excerptRecord.setStatus(IN_PROGRESS);
    var now = LocalDateTime.now();
    excerptRecord.setCreatedAt(now);
    excerptRecord.setUpdatedAt(now);
    excerptRecord.setSignatureRequired(excerptEventDto.isRequiresSystemSignature());
    return excerptRecord;
  }
}
