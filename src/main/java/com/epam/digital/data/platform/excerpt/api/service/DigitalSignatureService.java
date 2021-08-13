package com.epam.digital.data.platform.excerpt.api.service;

import com.epam.digital.data.platform.dso.api.dto.VerifyRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.excerpt.api.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidSignatureException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DigitalSignatureService {

  private static final String SIGNATURE = "signature";
  private final CephService requestSignatureCephService;
  private final CephService excerptSignatureCephService;
  private final String requestSignatureBucket;
  private final String excerptSignatureBucket;
  private final DigitalSealRestClient digitalSealRestClient;
  private final ObjectMapper objectMapper;

  public DigitalSignatureService(
      @Qualifier("requestSignatureCephService") CephService requestSignatureCephService,
      @Qualifier("excerptSignatureCephService") CephService excerptSignatureCephService,
      @Value("${request-signature-ceph.bucket}") String requestSignatureBucket,
      @Value("${excerpt-signature-ceph.bucket}") String excerptSignatureBucket,
      DigitalSealRestClient digitalSealRestClient,
      ObjectMapper objectMapper) {
    this.requestSignatureCephService = requestSignatureCephService;
    this.excerptSignatureCephService = excerptSignatureCephService;
    this.requestSignatureBucket = requestSignatureBucket;
    this.excerptSignatureBucket = excerptSignatureBucket;
    this.digitalSealRestClient = digitalSealRestClient;
    this.objectMapper = objectMapper;
  }

  public void checkSignature(ExcerptEventDto data, String key) {

    String responseFromCeph =
        requestSignatureCephService
            .getContent(requestSignatureBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException(
                    "Signature does not exist in ceph bucket. Key: " + key));

    String signature;
    String dataStr;
    try {
      Map<String, Object> cephResponse = objectMapper.readValue(responseFromCeph, Map.class);

      signature = (String) cephResponse.get(SIGNATURE);
      dataStr = objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    verify(signature, dataStr);
  }

  public String saveSignature(String key) {
    String value =
        requestSignatureCephService
            .getContent(requestSignatureBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException(
                    "Signature does not exist in ceph bucket. Key: " + key));
    excerptSignatureCephService.putContent(excerptSignatureBucket, key, value);
    return value;
  }

  private void verify(String signature, String data) {
    try {
      VerifyResponseDto responseDto = digitalSealRestClient.verify(
          new VerifyRequestDto(signature, data));

      if (!responseDto.isValid) {
        throw new InvalidSignatureException(responseDto.error.getMessage());
      }
    } catch (BadRequestException e) {
      throw new KepServiceBadRequestException(e.getMessage());
    } catch (InternalServerErrorException e) {
      throw new KepServiceInternalServerErrorException(e.getMessage());
    }
  }
}
