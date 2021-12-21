package com.epam.digital.data.platform.excerpt.api.config;

import brave.baggage.BaggageFields;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SleuthConfig {

  @Bean
  CurrentTraceContext.ScopeDecorator legacyIds() {
    return MDCScopeDecorator.newBuilder()
        .clear()
          .add(SingleCorrelationField.newBuilder(BaggageFields.TRACE_ID)
            .name("X-B3-TraceId").build())
        .add(SingleCorrelationField.newBuilder(BaggageFields.PARENT_ID)
            .name("X-B3-ParentSpanId").build())
        .add(SingleCorrelationField.newBuilder(BaggageFields.SPAN_ID)
            .name("X-B3-SpanId").build())
        .add(SingleCorrelationField.newBuilder(BaggageFields.SAMPLED)
            .name("X-Span-Export").build())
        .build();
  }
}
