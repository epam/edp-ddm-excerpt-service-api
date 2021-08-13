package com.epam.digital.data.platform.excerpt.api;

import com.epam.digital.data.platform.excerpt.api.annotation.HttpSecurityContext;
import io.swagger.v3.core.util.PrimitiveType;
import org.springdoc.core.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.epam.digital.data.platform.excerpt.dao")
@SpringBootApplication
public class ExcerptServiceApiApplication {

  static {
    SpringDocUtils.getConfig().addAnnotationsToIgnore(HttpSecurityContext.class);
    PrimitiveType.enablePartialTime();
  }

  public static void main(String[] args) {
    SpringApplication.run(ExcerptServiceApiApplication.class, args);
  }
}
