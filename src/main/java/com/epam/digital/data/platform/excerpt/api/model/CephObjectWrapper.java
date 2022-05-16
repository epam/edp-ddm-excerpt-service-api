/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.excerpt.api.model;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import java.util.Objects;

public class CephObjectWrapper {

  private final CephObject cephObject;
  private final String excerptType;

  public CephObjectWrapper(
      CephObject cephObject, String excerptType) {
    this.cephObject = cephObject;
    this.excerptType = excerptType;
  }

  public CephObject getCephObject() {
    return cephObject;
  }

  public String getExcerptType() {
    return excerptType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CephObjectWrapper that = (CephObjectWrapper) o;
    return cephObject.equals(that.cephObject) && excerptType.equals(that.excerptType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cephObject, excerptType);
  }
}
