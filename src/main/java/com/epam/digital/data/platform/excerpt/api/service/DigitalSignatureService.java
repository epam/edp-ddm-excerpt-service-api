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

import com.epam.digital.data.platform.dso.api.dto.VerificationRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.excerpt.api.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidSignatureException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.excerpt.api.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import com.epam.digital.data.platform.storage.form.service.FormDataStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigitalSignatureService {

  private static final String SIGNATURE = "signature";

  private final Logger log = LoggerFactory.getLogger(DigitalSignatureService.class);

  private final FormDataStorageService lowcodeFormDataStorageService;
  private final FormDataStorageService datafactoryExcerptDataStorageService;
  private final DigitalSealRestClient digitalSealRestClient;
  private final ObjectMapper objectMapper;

  public void checkSignature(ExcerptEventDto data, String key) {
    log.info("Retrieve Signature from Ceph");
    var formDataDto =
        lowcodeFormDataStorageService
            .getFormData(key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException(
                    "Signature does not exist in ceph bucket. Key: " + key));

    String dataStr;
    try {
      dataStr = objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    verify(formDataDto.getSignature(), dataStr);
  }

  public FormDataDto saveSignature(String key) {
    log.info("Store Signature to Ceph");
    var value =
        lowcodeFormDataStorageService
            .getFormData(key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException(
                    "Signature does not exist in ceph bucket. Key: " + key));
    datafactoryExcerptDataStorageService.putFormData(key, value);
    return value;
  }

  private void verify(String signature, String data) {
    try {
      log.info("Verify Signature");
      VerificationResponseDto responseDto = digitalSealRestClient.verify(
          new VerificationRequestDto(signature, data));

      if (!responseDto.isValid()) {
        throw new InvalidSignatureException(responseDto.getError().getMessage());
      }
    } catch (BadRequestException e) {
      throw new KepServiceBadRequestException(e.getMessage());
    } catch (InternalServerErrorException e) {
      throw new KepServiceInternalServerErrorException(e.getMessage());
    }
  }
}
