package com.itemis.maven.plugins.unleash.scm.requests;

public class DeleteBranchRequest {
  protected String message;
  protected boolean push;
  protected String branchName;

  DeleteBranchRequest() {
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
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

  public static class Builder {
    private DeleteBranchRequest request = new DeleteBranchRequest();

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

    public DeleteBranchRequest build() {
      return this.request;
    }
  }
}
