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

package com.epam.digital.data.platform.excerpt.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TestUtils {

  private TestUtils() {
  }

  public static String readClassPathResource(String path) throws IOException {
    var resource = TestUtils.class.getResourceAsStream(path);
    return new String(Objects.requireNonNull(resource).readAllBytes(), StandardCharsets.UTF_8);
  }
}
