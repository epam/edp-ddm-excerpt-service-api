package com.epam.digital.data.platform.excerpt.api.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.IN_PROGRESS;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.model.StatusDto;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.Header;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class ExcerptService {

  private final Logger log = LoggerFactory.getLogger(ExcerptService.class);

  private final RecordRepository recordRepository;
  private final TemplateRepository templateRepository;

  private final KafkaHelper kafkaHelper;
  private final JwtHelper jwtHelper;

  private final CephService excerptCephService;
  private final String bucket;

  private final DigitalSignatureService digitalSignatureService;

  public ExcerptService(
      RecordRepository recordRepository,
      TemplateRepository templateRepository,
      KafkaHelper kafkaHelper,
      JwtHelper jwtHelper,
      CephService excerptCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket,
      DigitalSignatureService digitalSignatureService) {
    this.recordRepository = recordRepository;
    this.templateRepository = templateRepository;
    this.kafkaHelper = kafkaHelper;
    this.jwtHelper = jwtHelper;
    this.excerptCephService = excerptCephService;
    this.bucket = bucket;
    this.digitalSignatureService = digitalSignatureService;
  }

  public ExcerptEntityId generateExcerpt(ExcerptEventDto excerptEventDto, SecurityContext context) {
    var excerptType = excerptEventDto.getExcerptType();

    validateAndSaveSignatures(excerptEventDto, context);
    validateTemplate(excerptType);

    var newRecord = recordRepository.save(buildRecord(excerptEventDto, context));
    kafkaHelper.send(newRecord, excerptType, excerptEventDto.getExcerptInputData());

    return new ExcerptEntityId(newRecord.getId());
  }

  private void validateAndSaveSignatures(ExcerptEventDto excerptEventDto, SecurityContext context) {
    verifyMandatoryHeaders(context);

    digitalSignatureService
        .checkSignature(excerptEventDto,
            context.getDigitalSignatureDerived());

    digitalSignatureService.saveSignature(context.getDigitalSignature());
    digitalSignatureService.saveSignature(context.getDigitalSignatureDerived());
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

    log.info("Find Excerpt in Ceph");
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
