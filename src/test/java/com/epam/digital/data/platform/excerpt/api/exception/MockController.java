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

package com.epam.digital.data.platform.excerpt.api.exception;

import com.epam.digital.data.platform.excerpt.api.audit.AuditableController;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock")
public class MockController {

  private final MockService mockService;

  public MockController(MockService mockService) {
    this.mockService = mockService;
  }

  @AuditableController(action = "GET EXCERPT")
  @GetMapping("/{id}")
  public ResponseEntity<Resource> getExcerpt(@PathVariable("id") UUID uuid) {
    var excerpt = mockService.getExcerpt(uuid);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(excerpt.getByteArray().length)
        .header("STUB", "STUB")
        .body(excerpt);
  }

  @AuditableController(action = "GENERATE EXCERPT")
  @PostMapping
  public ResponseEntity<ExcerptEntityId> generateExcerpt(@Valid @RequestBody MockEntity entity) {
    var id = mockService.generateExcerpt(entity);

    return ResponseEntity.ok().body(id);
  }

  @AuditableController(action = "DELETE EXCERPT")
  @DeleteMapping
  public ResponseEntity<Void> deleteExcerpt(@Valid @RequestBody MockEntity entity) {
    return ResponseEntity.ok().build();
  }
}
