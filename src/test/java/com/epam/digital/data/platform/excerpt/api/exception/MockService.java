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
