package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

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
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

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

  public Optional<String> getPreTagCommitMessage() {
    return Optional.fromNullable(this.preTagCommitMessage);
  }

  public boolean tagFromWorkingCopy() {
    return this.remoteRepositoryUrl == null;
  }

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
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
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for updates prior to pushing the tag.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for updates prior to pushing the tag.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for updates prior to pushing the tag.<br>
     * This will request real merging of conflicts between local and remote changes. In case of such a conflict the
     * {@link MergeClient} is used to resolve the conflict.<br>
     * <br>
     * Set the merge client using {@link #mergeClient(MergeClient)} when setting this merge strategy!
     *
     * @return the builder itself.
     */
    public Builder merge() {
      this.request.mergeStrategy = MergeStrategy.FULL_MERGE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for updates prior to pushing the tag.<br>
     * This will request to not merge local and remote changes which will likely result in failure messages or conflict
     * info being written.
     *
     * @return the builder itself.
     */
    public Builder noMerge() {
      this.request.mergeStrategy = MergeStrategy.DO_NOT_MERGE;
      return this;
    }

    /**
     * Sets a specific merge strategy for merge conflicts during updates prior to pushing the tag.
     *
     * @param mergeStrategy the requested merge strategy. {@code null} results in {@link MergeStrategy#DO_NOT_MERGE}.
     * @return the builder itself.
     */
    public Builder mergeStrategy(MergeStrategy mergeStrategy) {
      if (mergeStrategy != null) {
        this.request.mergeStrategy = mergeStrategy;
      } else {
        noMerge();
      }
      return this;
    }

    /**
     * @param mergeClient the merge client to be used in case of merge conflicts and merge strategy
     *          {@link MergeStrategy#FULL_MERGE}.
     * @return the builder itself.
     */
    public Builder mergeClient(MergeClient mergeClient) {
      this.request.mergeClient = mergeClient;
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
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
