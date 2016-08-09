package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

/**
 * A Request for the creation of branches from either the local working copy of the repository or a remote repository
 * path.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Specify a remote repository URL and optionally a revision to branch from that URL.
 * <ul>
 * <li>The working directory of the SCM provider may or may not exist and may not be connected to a remote repository.
 * <li>If no revision is specified, HEAD is assumed.</li>
 * </li>
 * </ul>
 * </li>
 * <li>Do not specify the remote URL to branch from the local working copy the {@link ScmProvider} is configured with.
 * </li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class BranchRequest {
  private String remoteRepositoryUrl;
  private String revision;
  private String message;
  private boolean push;
  private String branchName;
  private boolean commitBeforeBranching;
  private String preBranchCommitMessage;
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  private BranchRequest() {
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

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
  }

  /**
   * The builder for a {@link BranchRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private BranchRequest request = new BranchRequest();

    /**
     * @param remoteRepositoryUrl the remote URL to branch from. If this url is omitted, branching happens from the
     *          local working directory.
     * @return the builder itself.
     */
    public Builder from(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    /**
     * @param revision the revision to branch from. This makes only sense if branching is requested from remote.
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
     * @param branchName the name of the branch that shall be created (mandatory).
     * @return the builder itself.
     */
    public Builder branchName(String branchName) {
      this.request.branchName = branchName;
      return this;
    }

    /**
     * Request committing local changes prior to branching from the local repository.
     *
     * @return the builder itself.
     */
    public Builder commitBeforeBranching() {
      this.request.commitBeforeBranching = true;
      return this;
    }

    /**
     * @param message the repository log message for the pre-branch commit (mandatory if pre-branch committing is
     *          requested).
     * @return the builder itself.
     */
    public Builder preBranchCommitMessage(String message) {
      this.request.preBranchCommitMessage = message;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for updates prior to pushing the branch.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for updates prior to pushing the branch.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for updates prior to pushing the branch.<br>
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
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for updates prior to pushing the branch.<br>
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
     * Sets a specific merge strategy for merge conflicts during updates prior to pushing the branch.
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
     * Checks the settings of the request to build and builds the actual branching request.
     *
     * @return the request for branching the repository.
     */
    public BranchRequest build() {
      Preconditions.checkState(!Strings.isNullOrEmpty(this.request.branchName), "No branch name specified!");
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      if (this.request.commitBeforeBranching) {
        Preconditions.checkState(this.request.preBranchCommitMessage != null,
            "Committing before branching has been requested but no pre-branch commit message has been specified!");
      }
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
