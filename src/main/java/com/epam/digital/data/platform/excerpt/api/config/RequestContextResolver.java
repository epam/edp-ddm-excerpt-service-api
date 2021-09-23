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
