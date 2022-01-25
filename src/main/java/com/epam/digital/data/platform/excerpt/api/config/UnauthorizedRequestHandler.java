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

import com.epam.digital.data.platform.starter.security.jwt.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UnauthorizedRequestHandler extends RestAuthenticationEntryPoint {

  private final HandlerExceptionResolver handlerExceptionResolver;

  public UnauthorizedRequestHandler(ObjectMapper objectMapper, HandlerExceptionResolver handlerExceptionResolver) {
    super(objectMapper);
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException e) {
    handlerExceptionResolver.resolveException(request, response, null, e);
  }
}
