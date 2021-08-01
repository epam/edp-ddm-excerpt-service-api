package com.epam.digital.data.platform.excerpt.api.exception;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.ByteArrayResource;

@TestComponent
public class MockService {

  public ByteArrayResource getExcerpt(UUID uuid) {
    return mock(ByteArrayResource.class);
  }

  public ExcerptEntityId generateExcerpt(MockEntity dto) {
    return mock(ExcerptEntityId.class);
  }

}
