package com.epam.digital.data.platform.excerpt.api.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.repository.TemplateRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.epam.digital.data.platform.integration.ceph.dto.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcerptServiceTest {

  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  static final String BUCKET = "BUCKET";
  static final byte[] CEPH_CONTENT = "test".getBytes();

  ExcerptService instance;

  @Mock
  RecordRepository recordRepository;

  @Mock
  TemplateRepository templateRepository;

  @Mock
  KafkaHelper kafkaHelper;

  @Mock
  CephService excerptCephService;

  @Mock
  JwtHelper jwtHelper;

  @BeforeEach
  void setup() {
    instance = new ExcerptService(recordRepository, templateRepository, kafkaHelper,
        jwtHelper, excerptCephService, BUCKET);
  }

  @Nested
  class Get {

    @Test
    void failWhenCephServiceFails() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");

      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(Optional.empty());
      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");

      assertThrows(ExcerptNotFoundException.class, () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void failWhenKeycloakIdDoesNotMatch() {
      var record = new ExcerptRecord();
      record.setKeycloakId("incorrectId");

      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");

      assertThrows(InvalidKeycloakIdException.class, () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void failWhenNoRecordFound() {
      assertThrows(ExcerptNotFoundException.class, () -> instance.getExcerpt(ID, any()));
    }

    @Test
    void failWhenNotFoundInCeph() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");

      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");
      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(Optional.empty());

      assertThrows(ExcerptNotFoundException.class, () -> instance.getExcerpt(ID, securityContext()));
    }

    @Test
    void returnResource() {
      var record = new ExcerptRecord();
      record.setKeycloakId("stubId");

      when(jwtHelper.getKeycloakId(any())).thenReturn("stubId");
      when(recordRepository.findById(any())).thenReturn(Optional.of(record));
      when(excerptCephService.getObject(any(), any())).thenReturn(Optional.of(new CephObject(CEPH_CONTENT, Map.of())));

      var resource = instance.getExcerpt(ID, securityContext());

      assertThat(resource.getByteArray()).isEqualTo(CEPH_CONTENT);
    }
  }

  @Nested
  class Status {

    @Test
    void failWhenRecordNotFound() {
      assertThrows(ExcerptNotFoundException.class, () -> instance.getStatus(ID));
    }

    @Test
    void returnStatus() {
      var expectedStatus = FAILED;
      var expectedDetails = "some details";
      setupStatusFound(expectedStatus, expectedDetails);

      var status = instance.getStatus(ID);

      assertThat(status.getStatus()).isEqualTo(expectedStatus);
      assertThat(status.getStatusDetails()).isEqualTo(expectedDetails);
    }
  }

  @Nested
  class Generate {

    @Test
    void failWhenTemplateTypeNotFound() {
      assertThrows(ExcerptProcessingException.class, () -> instance.generateExcerpt(buildExcerptEvent(), new SecurityContext()));
    }

    @Test
    void returnEntityId() {
      setupExcerptFound(ID);

      var entityId = instance.generateExcerpt(buildExcerptEvent(), new SecurityContext());

      verify(recordRepository).save(any());
      verify(kafkaHelper).send(any(), any(), any());
      assertThat(entityId.getExcerptIdentifier()).isEqualTo(ID);
    }
  }

  private void setupStatusFound(ExcerptProcessingStatus status, String details) {
    var record = new ExcerptRecord();
    record.setStatus(status);
    record.setStatusDetails(details);
    when(recordRepository.findById(any())).thenReturn(Optional.of(record));
  }

  private void setupExcerptFound(UUID id) {
    var record = new ExcerptRecord();
    record.setId(id);
    when(templateRepository.findFirstByTemplateName("test_type")).thenReturn(Optional.of(new ExcerptTemplate()));
    when(recordRepository.save(any())).thenReturn(record);
  }

  private ExcerptEventDto buildExcerptEvent() {
    return new ExcerptEventDto(ID, "test_type", new HashMap<>(), false);
  }

  private SecurityContext securityContext() {
    var context = new SecurityContext();
    context.setAccessToken("stub");
    return context;
  }
}
