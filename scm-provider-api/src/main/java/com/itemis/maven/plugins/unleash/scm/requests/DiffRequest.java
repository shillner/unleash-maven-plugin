package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;

/**
 * A Request for the creation of a diff between two revisions of the current repository (working dir) or remote ones.
 * Note that the remote repository option may not be supported by all SCM types.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Use the repository that is checked out in your current working directory for the diff creation.</li>
 * <li>Specify repository URLs to create the diff remotely from these urls. This may not be supported by all SCMs!</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.3.0
 */
public class DiffRequest {
  private String sourceRemoteRepositoryUrl;
  private String targetRemoteRepositoryUrl;
  private String sourceRevision;
  private String targetRevision;
  private DiffType type = DiffType.FULL;

  private DiffRequest() {
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<String> getSourceRemoteRepositoryUrl() {
    return Optional.fromNullable(this.sourceRemoteRepositoryUrl);
  }

  public Optional<String> getTargetRemoteRepositoryUrl() {
    return Optional.fromNullable(this.targetRemoteRepositoryUrl);
  }

  public Optional<String> getSourceRevision() {
    return Optional.fromNullable(this.sourceRevision);
  }

  public Optional<String> getTargetRevision() {
    return Optional.fromNullable(this.targetRevision);
  }

  public DiffType getType() {
    return this.type;
  }

  /**
   * The builder for a {@link DiffRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.3.0
   */
  public static class Builder {
    private DiffRequest request = new DiffRequest();

    /**
     * @param sourceRemoteRepositoryUrl the remote URL which shall be the source of the diff creation (left side). If
     *          this url is omitted, the source of the diff will be retrieved locally or from the remote repository from
     *          which the local working directory has been cloned. Note that some SCM providers may not support diff
     *          creation from remote repositories. In this case those implementations may throw an exception.
     * @return the builder itself.
     */
    public Builder sourceRemoteUrl(String sourceRemoteRepositoryUrl) {
      this.request.sourceRemoteRepositoryUrl = sourceRemoteRepositoryUrl;
      return this;
    }

    /**
     * @param targetRemoteRepositoryUrl the remote URL which shall be the target of the diff creation (right side). If
     *          this url is omitted, the target of the diff will be retrieved locally or from the remote repository from
     *          which the local working directory has been cloned. Note that some SCM providers may not support diff
     *          creation from remote repositories. In this case those implementations may throw an exception.
     * @return the builder itself.
     */
    public Builder targetRemoteUrl(String targetRemoteRepositoryUrl) {
      this.request.targetRemoteRepositoryUrl = targetRemoteRepositoryUrl;
      return this;
    }

    /**
     * The source revision from which to start the diff creation. This can be the SVN revision number or the SHA1 hash
     * of a git commit.
     *
     * @param sourceRevision the revision to start the diff creation from.
     * @return the builder itself.
     */
    public Builder sourceRevision(String sourceRevision) {
      this.request.sourceRevision = sourceRevision;
      return this;
    }

    /**
     * The target revision of the diff creation. This can be the SVN revision number or the SHA1 hash of a git commit.
     *
     * @param targetRevision the target revision of the diff creation.
     * @return the builder itself.
     */
    public Builder targetRevision(String targetRevision) {
      this.request.targetRevision = targetRevision;
      return this;
    }

    /**
     * Requests that the diff only contains status information such as which files have been added, removed or changed.
     * No textual diff will be performed and the result will only be filled with the status information.
     *
     * @return the builder itself.
     */
    public Builder statusOnly() {
      this.request.type = DiffType.STATUS_ONLY;
      return this;
    }

    /**
     * Requests a full diff containing diff status information as well as a full textual diff for all changed objects.
     *
     * @return the builder itself.
     */
    public Builder fullDiff() {
      this.request.type = DiffType.FULL;
      return this;
    }

    /**
     * Requests a diff containing diff status information as well as a full textual diff only for changed objects. Added
     * or removed objects are omitted here.
     *
     * @return the builder itself.
     */
    public Builder changesOnly() {
      this.request.type = DiffType.CHANGES_ONLY;
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual diff request.
     *
     * @return the request for a diff of the repository paths.
     */
    public DiffRequest build() {
      return this.request;
    }
  }

  /**
   * Enumerates some diff calculation strategies.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.3.0
   */
  public static enum DiffType {
    /**
     * Status information only without textual file diffs.
     */
    STATUS_ONLY,
    /**
     * Status information as well as full textual file diffs.
     */
    FULL,
    /**
     * Status information as well as textual file diffs for modified files only.
     */
    CHANGES_ONLY;
  }
}
