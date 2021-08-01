package com.epam.digital.data.platform.excerpt.api.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.Pattern;

public class MockEntity {
  private UUID recordId;

  @Pattern(regexp = "^[a-zA-Z]{6,10}$")
  private String excerptType;

  private LocalDateTime updatedAt;

  public UUID getRecordId() {
    return recordId;
  }

  public void setRecordId(UUID recordId) {
    this.recordId = recordId;
  }

  public String getExcerptType() {
    return excerptType;
  }

  public void setExcerptType(String excerptType) {
    this.excerptType = excerptType;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
