package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;

public class BranchRequest {
  private String remoteRepositoryUrl;
  private String revision;
  protected String message;
  protected boolean push;
  protected String branchName;
  protected boolean commitBeforeBranching;
  protected String preBranchCommitMessage;

  BranchRequest() {
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<String> getRemoteRepositoryUrl() {
    return Optional.fromNullable(this.remoteRepositoryUrl);
  }

  public Optional<String> getRevision() {
    return Optional.fromNullable(this.revision);
  }

  public String getMessage() {
    return this.message;
  }

  public boolean push() {
    return this.push;
  }

  public String getBranchName() {
    return this.branchName;
  }

  public boolean commitBeforeBranching() {
    return this.commitBeforeBranching;
  }

  public String getPreBranchCommitMessage() {
    return this.preBranchCommitMessage;
  }

  public boolean branchFromWorkingCopy() {
    return this.remoteRepositoryUrl == null;
  }

  public static class Builder {
    private BranchRequest request = new BranchRequest();

    public Builder from(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    public Builder revision(String revision) {
      this.request.revision = revision;
      return this;
    }

    public Builder message(String message) {
      this.request.message = message;
      return this;
    }

    public Builder push() {
      this.request.push = true;
      return this;
    }

    public Builder branchName(String branchName) {
      this.request.branchName = branchName;
      return this;
    }

    public Builder commitBeforeBranching() {
      this.request.commitBeforeBranching = true;
      return this;
    }

    public Builder preBranchCommitMessage(String message) {
      this.request.preBranchCommitMessage = message;
      return this;
    }

    public BranchRequest build() {
      return this.request;
    }
  }
}
