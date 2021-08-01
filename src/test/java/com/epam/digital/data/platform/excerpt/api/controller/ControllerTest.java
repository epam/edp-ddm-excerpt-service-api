package com.epam.digital.data.platform.excerpt.api.controller;

import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@TestPropertySource(properties = {"platform.security.enabled=false"})
@Import({PermitAllWebSecurityConfig.class})
@ContextConfiguration
public @interface ControllerTest {

  @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
  Class<?>[] value() default {};
}
