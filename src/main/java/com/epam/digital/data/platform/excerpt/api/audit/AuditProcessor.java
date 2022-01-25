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

package com.epam.digital.data.platform.excerpt.api.audit;

import com.epam.digital.data.platform.excerpt.api.exception.AuditException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Arrays;
import java.util.Objects;

public interface AuditProcessor<O> {

  Object process(ProceedingJoinPoint joinPoint, O operation) throws Throwable;

  default <T> T getArgumentByType(JoinPoint joinPoint, Class<T> clazz) {
    long numberOfArgumentsOfTheSameType = Arrays.stream(joinPoint.getArgs())
        .filter(Objects::nonNull)
        .filter(x -> x.getClass().equals(clazz))
        .count();

    if (numberOfArgumentsOfTheSameType != 1) {
      throw new AuditException("The number of arguments of the given type is not equal to one");
    }
    return (T) Arrays.stream(joinPoint.getArgs())
        .filter(Objects::nonNull)
        .filter(x -> x.getClass().equals(clazz))
        .findFirst().get();
  }
}
