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
import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
public class ControllerAuditAspect {

  private static final Set<Integer> httpStatusOfSecurityAudit = Set.of(401, 403);

  private static final Set<Class<? extends Annotation>> httpAnnotations =
      Set.of(GetMapping.class, PostMapping.class);

  // step
  static final String BEFORE = "BEFORE";
  static final String AFTER = "AFTER";

  private final RestAuditEventsFacade restAuditEventsFacade;

  public ControllerAuditAspect(RestAuditEventsFacade restAuditEventsFacade) {
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Pointcut("@annotation(com.epam.digital.data.platform.excerpt.api.audit.AuditableException)")
  public void auditableExceptionPointcut() {
  }

  @Pointcut("@annotation(com.epam.digital.data.platform.excerpt.api.audit.AuditableController)")
  public void controller() {
  }

  @AfterReturning(pointcut = "auditableExceptionPointcut()", returning = "response")
  void exceptionAudit(JoinPoint joinPoint, ResponseEntity<?> response) {
    var auditableExceptionAnnotation = ((MethodSignature) joinPoint.getSignature())
        .getMethod()
        .getAnnotation(AuditableException.class);
    prepareAndSendExceptionAudit(response, auditableExceptionAnnotation);
  }

  @Around("controller() && args(object,..)")
  Object controllerAudit(ProceedingJoinPoint joinPoint, Object object) throws Throwable {

    var auditableControllerAnnotation = ((MethodSignature) joinPoint.getSignature())
            .getMethod()
            .getAnnotation(AuditableController.class);
    var httpAnnotation = getAnnotation(joinPoint);

    if (httpAnnotation.equals(GetMapping.class) && object instanceof UUID) {
      return prepareAndSendRestAudit(
          joinPoint, auditableControllerAnnotation.action(), (UUID) object);
    } else if (httpAnnotation.equals(PostMapping.class)) {
      return prepareAndSendRestAudit(
          joinPoint, auditableControllerAnnotation.action(), null);
    } else {
      throw new AuditException("Cannot save audit for this HTTP method. Not supported annotation: @"
          + httpAnnotation.getSimpleName());
    }
  }

  private Class<? extends Annotation> getAnnotation(ProceedingJoinPoint joinPoint) {
    var annotations = Arrays.stream(((MethodSignature) joinPoint.getSignature())
            .getMethod()
            .getAnnotations())
        .map(Annotation::annotationType)
        .collect(Collectors.toCollection(ArrayList::new));

    annotations.retainAll(httpAnnotations);
    if (annotations.size() != 1) {
      throw new AuditException(
          String.format(
              "The request handler must have exactly one mapping annotation, but has %d: %s", 
              annotations.size(), annotations));
    }
    return annotations.get(0);
  }

  private void prepareAndSendExceptionAudit(ResponseEntity<?> response,
      AuditableException auditableException) {
    var exceptionAuditEvent = new ExceptionAuditEvent();
    String action;
    if (response.getBody() instanceof DetailedErrorResponse) {
      action = ((DetailedErrorResponse) response.getBody()).getCode();
    } else {
      action = response.getStatusCode().getReasonPhrase();
    }
    exceptionAuditEvent.setAction(action);

    EventType eventType;
    if (httpStatusOfSecurityAudit.contains(response.getStatusCodeValue())) {
      eventType = EventType.SECURITY_EVENT;
    } else {
      eventType = EventType.USER_ACTION;
    }
    exceptionAuditEvent.setEventType(eventType);
    exceptionAuditEvent.setUserInfoEnabled(auditableException.userInfoEnabled());

    restAuditEventsFacade.sendExceptionAudit(exceptionAuditEvent);
  }

  private Object prepareAndSendRestAudit(ProceedingJoinPoint joinPoint, String action, UUID id) throws Throwable {

    String methodName = joinPoint.getSignature().getName();

    restAuditEventsFacade
        .sendRestAudit(EventType.USER_ACTION, methodName, action, BEFORE, id, null);

    Object result = joinPoint.proceed();

    var resultStatus = ((ResponseEntity<?>) result).getStatusCode().getReasonPhrase();

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, methodName,
        action, AFTER, id, resultStatus);

    return result;
  }
}
