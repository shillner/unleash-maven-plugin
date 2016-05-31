package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A Request for deleting a branch of the repository.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class DeleteBranchRequest {
  protected String message;
  protected boolean push;
  protected String branchName;

  private DeleteBranchRequest() {
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

  /**
   * The builder for a {@link DeleteBranchRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private DeleteBranchRequest request = new DeleteBranchRequest();

    /**
     * @param message the repository log message (mandatory).
     * @return the builder itself.
     */
    public Builder message(String message) {
      this.request.message = message;
      return this;
    }

    /**
     * Request pushing to the remote repository in case of distributed SCMs.
     *
     * @return the builder itself.
     */
    public Builder push() {
      this.request.push = true;
      return this;
    }

    /**
     * @param branchName the name of the branch that shall be deleted (mandatory).
     * @return the builder itself.
     */
    public Builder branchName(String branchName) {
      this.request.branchName = branchName;
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual branch deletion request.
     *
     * @return the request for deleting a repository branch.
     */
    public DeleteBranchRequest build() {
      Preconditions.checkState(!Strings.isNullOrEmpty(this.request.branchName), "No branch name specified!");
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      return this.request;
    }
  }
}
