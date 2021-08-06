package com.epam.digital.data.platform.excerpt.api.util;

import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import org.springframework.stereotype.Component;

@Component
public class JwtHelper {

  private final TokenParser tokenParser;

  public JwtHelper(TokenParser tokenParser) {
    this.tokenParser = tokenParser;
  }

  public String getKeycloakId(String accessToken) {
    return tokenParser.parseClaims(accessToken).getSubject();
  }
}
