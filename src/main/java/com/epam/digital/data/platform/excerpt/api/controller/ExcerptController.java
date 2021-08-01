package com.epam.digital.data.platform.excerpt.api.controller;

import com.epam.digital.data.platform.excerpt.api.model.StatusDto;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptService;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import java.util.UUID;
import javax.validation.Valid;
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
  private static final String ATTACHMENT_HEADER_VALUE = "attachment; filename=\"%s.pdf\"";

  private final ExcerptService excerptService;

  public ExcerptController(
      ExcerptService excerptService) {
    this.excerptService = excerptService;
  }

  @PostMapping
  public ResponseEntity<ExcerptEntityId> generate(@Valid @RequestBody ExcerptEventDto excerptEventDto) {
    return ResponseEntity.ok().body(excerptService.generateExcerpt(excerptEventDto));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> retrieve(@PathVariable("id") UUID id) {
    var excerpt = excerptService.getExcerpt(id);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(excerpt.getByteArray().length)
        .header(CONTENT_DISPOSITION_HEADER_NAME, String.format(ATTACHMENT_HEADER_VALUE, id.toString()))
        .body(excerpt);
  }

  @GetMapping("/{id}/status")
  public ResponseEntity<StatusDto> status(@PathVariable("id") UUID id) {
    var status = excerptService.getStatus(id);
    return ResponseEntity.ok().body(status);
  }
}
