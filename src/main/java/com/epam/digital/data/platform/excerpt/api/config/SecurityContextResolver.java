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

import com.epam.digital.data.platform.excerpt.api.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.util.Header;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class SecurityContextResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterAnnotation(HttpSecurityContext.class) != null;

  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    var context = new SecurityContext();
    context.setAccessToken(webRequest.getHeader(Header.ACCESS_TOKEN.getHeaderName()));
    context.setDigitalSignature(webRequest.getHeader(Header.X_DIGITAL_SIGNATURE.getHeaderName()));
    context.setDigitalSignatureDerived(
        webRequest.getHeader(Header.X_DIGITAL_SIGNATURE_DERIVED.getHeaderName()));
    return context;
  }
}
