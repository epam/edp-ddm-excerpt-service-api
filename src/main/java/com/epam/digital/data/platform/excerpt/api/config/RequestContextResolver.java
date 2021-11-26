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

package com.epam.digital.data.platform.excerpt.api.config;

import com.epam.digital.data.platform.excerpt.api.annotation.HttpRequestContext;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.util.Header;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class RequestContextResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterAnnotation(HttpRequestContext.class) != null;
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    RequestContext context = new RequestContext();
    context.setSourceSystem(webRequest.getHeader(Header.X_SOURCE_SYSTEM.getHeaderName()));
    context.setSourceApplication(
        webRequest.getHeader(Header.X_SOURCE_APPLICATION.getHeaderName()));
    context.setSourceBusinessProcess(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName()));

    context.setSourceBusinessActivity(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_ACTIVITY.getHeaderName()));

    return context;
  }
}
