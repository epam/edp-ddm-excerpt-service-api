package com.epam.digital.data.platform.excerpt.api;

import io.swagger.v3.core.util.PrimitiveType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.epam.digital.data.platform.excerpt.dao")
@SpringBootApplication
public class ExcerptServiceApiApplication {

  static {
    PrimitiveType.enablePartialTime();
  }

  public static void main(String[] args) {
    SpringApplication.run(ExcerptServiceApiApplication.class, args);
  }
}
