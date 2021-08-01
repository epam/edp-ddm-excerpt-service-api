package com.epam.digital.data.platform.excerpt.api.model;

import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class StatusDto {

  private ExcerptProcessingStatus status;
  private String statusDetails;

  public StatusDto(ExcerptProcessingStatus status, String statusDetails) {
    this.status = status;
    this.statusDetails = statusDetails;
  }

  public ExcerptProcessingStatus getStatus() {
    return status;
  }

  public void setStatus(ExcerptProcessingStatus status) {
    this.status = status;
  }

  public String getStatusDetails() {
    return statusDetails;
  }

  public void setStatusDetails(String statusDetails) {
    this.statusDetails = statusDetails;
  }

  @Override
  public String toString() {
    return "StatusDto{" +
        "status=" + status +
        ", statusDetails='" + statusDetails + '\'' +
        '}';
  }
}
