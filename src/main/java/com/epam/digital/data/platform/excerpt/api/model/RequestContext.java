package com.epam.digital.data.platform.excerpt.api.model;

public class RequestContext {

  private String sourceSystem;
  private String sourceApplication;
  private String sourceBusinessProcess;
  private String sourceBusinessActivity;

  public String getSourceSystem() {
    return sourceSystem;
  }

  public void setSourceSystem(String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }

  public String getSourceApplication() {
    return sourceApplication;
  }

  public void setSourceApplication(String sourceApplication) {
    this.sourceApplication = sourceApplication;
  }

  public String getSourceBusinessProcess() {
    return sourceBusinessProcess;
  }

  public void setSourceBusinessProcess(String sourceBusinessProcess) {
    this.sourceBusinessProcess = sourceBusinessProcess;
  }

  public String getSourceBusinessActivity() {
    return sourceBusinessActivity;
  }

  public void setSourceBusinessActivity(String sourceBusinessActivity) {
    this.sourceBusinessActivity = sourceBusinessActivity;
  }
}
