package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A Request for deleting a tag of the repository.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class DeleteTagRequest {
  private String message;
  private boolean push;
  private String tagName;

  private DeleteTagRequest() {
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

  public String getTagName() {
    return this.tagName;
  }

  /**
   * The builder for a {@link DeleteTagRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private DeleteTagRequest request = new DeleteTagRequest();

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
     * @param tagName the name of the tag that shall be deleted (mandatory).
     * @return the builder itself.
     */
    public Builder tagName(String tagName) {
      this.request.tagName = tagName;
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual branch deletion request.
     *
     * @return the request for deleting a repository branch.
     */
    public DeleteTagRequest build() {
      Preconditions.checkState(!Strings.isNullOrEmpty(this.request.tagName), "No tag name specified!");
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      return this.request;
    }
  }
}
