package com.sukanth.dropbox.pojo;

/**
 * @apiNote pojo to hold application properties data
 */
public class DropBoxApplicationProperties {
  private String sourceLocation;
  private String destinationLocation;
  private boolean isIncludedDeleted;
  private int threadPooSize;
  private String accessToken;
  private String clientIdentifier;

  public String getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(String sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public String getDestinationLocation() {
    return destinationLocation;
  }

  public void setDestinationLocation(String destinationLocation) {
    this.destinationLocation = destinationLocation;
  }

  public boolean isIncludedDeleted() {
    return isIncludedDeleted;
  }

  public void setIncludedDeleted(boolean includedDeleted) {
    isIncludedDeleted = includedDeleted;
  }

  public int getThreadPooSize() {
    return threadPooSize;
  }

  public void setThreadPooSize(int threadPooSize) {
    this.threadPooSize = threadPooSize;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getClientIdentifier() {
    return clientIdentifier;
  }

  public void setClientIdentifier(String clientIdentifier) {
    this.clientIdentifier = clientIdentifier;
  }
}
