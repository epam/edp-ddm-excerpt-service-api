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

import com.epam.digital.data.platform.excerpt.api.exception.ExcerptNotFoundException;
import com.epam.digital.data.platform.excerpt.api.exception.InvalidKeycloakIdException;
import com.epam.digital.data.platform.excerpt.api.model.CephObjectWrapper;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.repository.RecordRepository;
import com.epam.digital.data.platform.excerpt.api.util.JwtHelper;
import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExcerptRetrievingService {

  private final Logger log = LoggerFactory.getLogger(ExcerptRetrievingService.class);

  private final RecordRepository recordRepository;
  private final JwtHelper jwtHelper;

  private final CephService excerptCephService;
  private final String bucket;

  public ExcerptRetrievingService(
      RecordRepository recordRepository,
      JwtHelper jwtHelper,
      CephService excerptCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket) {
    this.recordRepository = recordRepository;
    this.jwtHelper = jwtHelper;
    this.excerptCephService = excerptCephService;
    this.bucket = bucket;
  }

  public CephObjectWrapper getExcerpt(UUID id, SecurityContext context) {
    var excerpt = recordRepository.findById(id)
            .orElseThrow(() -> new ExcerptNotFoundException("Record not found in DB: " + id));

    validateKeycloakId(excerpt, context);

    log.info("Searching Excerpt in Ceph");
    if (excerpt.getExcerptKey() == null) {
      log.error("Could not find excerpt with null Ceph key");
      throw new ExcerptNotFoundException("Could not find excerpt with null Ceph key");
    }
    CephObject cephObject = excerptCephService
        .get(bucket, excerpt.getExcerptKey())
        .orElseThrow(() -> new ExcerptNotFoundException(
                    "Excerpt not found in Ceph: " + excerpt.getExcerptKey()));
    return new CephObjectWrapper(cephObject, excerpt.getExcerptType());
  }

  private void validateKeycloakId(ExcerptRecord excerpt, SecurityContext context) {
    var requestKeycloakId = jwtHelper.getKeycloakId(context.getAccessToken());

    if (!excerpt.getKeycloakId().equals(requestKeycloakId)) {
      throw new InvalidKeycloakIdException("KeycloakId does not match one stored in database");
    }
  }
}
