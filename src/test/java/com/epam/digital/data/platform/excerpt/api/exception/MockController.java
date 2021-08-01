package com.epam.digital.data.platform.excerpt.api.exception;

import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
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
@RequestMapping("/mock")
public class MockController {

  private MockService mockService;

  public MockController(MockService mockService) {
    this.mockService = mockService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> getExcerpt(@PathVariable("id") UUID uuid) {
    var excerpt = mockService.getExcerpt(uuid);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(excerpt.getByteArray().length)
        .header("STUB", "STUB")
        .body(excerpt);
  }

  @PostMapping
  public ResponseEntity<ExcerptEntityId> generateExcerpt(@Valid @RequestBody MockEntity entity) {
    var id = mockService.generateExcerpt(entity);

    return ResponseEntity.ok().body(id);
  }
}
