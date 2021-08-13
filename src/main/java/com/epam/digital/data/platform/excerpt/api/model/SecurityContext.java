package com.epam.digital.data.platform.excerpt.api.model;

import java.util.Objects;

public class SecurityContext {
  private String accessToken;
  private String digitalSignature;
  private String digitalSignatureDerived;

  public SecurityContext() {
  }

  public SecurityContext(String accessToken, String digitalSignature,
      String digitalSignatureDerived) {
    this.accessToken = accessToken;
    this.digitalSignature = digitalSignature;
    this.digitalSignatureDerived = digitalSignatureDerived;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getDigitalSignature() {
    return digitalSignature;
  }

  public void setDigitalSignature(String digitalSignature) {
    this.digitalSignature = digitalSignature;
  }

  public String getDigitalSignatureDerived() {
    return digitalSignatureDerived;
  }

  public void setDigitalSignatureDerived(String digitalSignatureDerived) {
    this.digitalSignatureDerived = digitalSignatureDerived;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SecurityContext that = (SecurityContext) o;
    return Objects.equals(accessToken, that.accessToken) && Objects.equals(
        digitalSignature, that.digitalSignature) && Objects.equals(digitalSignatureDerived,
        that.digitalSignatureDerived);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, digitalSignature, digitalSignatureDerived);
  }
}
