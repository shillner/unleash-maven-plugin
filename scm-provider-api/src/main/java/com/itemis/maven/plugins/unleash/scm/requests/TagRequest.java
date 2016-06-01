package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;

/**
 * A Request for the creation of tags from either the local working copy of the repository or a remote repository
 * path.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Specify a remote repository URL and optionally a revision to tag from that URL.
 * <ul>
 * <li>The working directory of the SCM provider may or may not exist and may not be connected to a remote repository.
 * <li>If no revision is specified, HEAD is assumed.</li>
 * </li>
 * </ul>
 * </li>
 * <li>Do not specify the remote URL to tag from the local working copy the {@link ScmProvider} is configured with.
 * </li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class TagRequest {
  private String remoteRepositoryUrl;
  private String revision;
  protected String message;
  protected boolean push;
  protected String tagName;
  protected boolean commitBeforeTagging;
  protected String preTagCommitMessage;

  private TagRequest() {
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

  public String getTagName() {
    return this.tagName;
  }

  public boolean commitBeforeTagging() {
    return this.commitBeforeTagging;
  }

  public String getPreTagCommitMessage() {
    return this.preTagCommitMessage;
  }

  public boolean tagFromWorkingCopy() {
    return this.remoteRepositoryUrl == null;
  }

  /**
   * The builder for a {@link TagRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private TagRequest request = new TagRequest();

    /**
     * @param remoteRepositoryUrl the remote URL to tag from. If this url is omitted, tagging happens from the
     *          local working directory.
     * @return the builder itself.
     */
    public Builder from(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    /**
     * @param revision the revision to tag from. This makes only sense if tagging is requested from remote.
     * @return the builder itself.
     */
    public Builder revision(String revision) {
      this.request.revision = revision;
      return this;
    }

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
     * @param tagName the name of the tag that shall be created (mandatory).
     * @return the builder itself.
     */
    public Builder tagName(String tagName) {
      this.request.tagName = tagName;
      return this;
    }

    /**
     * Request committing local changes prior to tagging from the local repository.
     *
     * @return the builder itself.
     */
    public Builder commitBeforeTagging() {
      this.request.commitBeforeTagging = true;
      return this;
    }

    /**
     * @param message the repository log message for the pre-tag commit (mandatory if pre-tag committing is
     *          requested).
     * @return the builder itself.
     */
    public Builder preTagCommitMessage(String message) {
      this.request.preTagCommitMessage = message;
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual tagging request.
     *
     * @return the request for tagging the repository.
     */
    public TagRequest build() {
      Preconditions.checkState(!Strings.isNullOrEmpty(this.request.tagName), "No tag name specified!");
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      if (this.request.commitBeforeTagging) {
        Preconditions.checkState(this.request.preTagCommitMessage != null,
            "Committing before tagging has been requested but no pre-tag commit message has been specified!");
      }
      return this.request;
    }
  }
}
