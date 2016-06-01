package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

/**
 * A Request for reverting a number of commits either locally or remotely.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class RevertCommitsRequest {
  private String fromRevision;
  private String toRevision;
  private String message;
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  private RevertCommitsRequest() {
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getFromRevision() {
    return this.fromRevision;
  }

  public String getToRevision() {
    return this.toRevision;
  }

  public String getMessage() {
    return this.message;
  }

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
  }

  /**
   * The builder for a {@link RevertCommitsRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private RevertCommitsRequest request = new RevertCommitsRequest();

    /**
     * @param revision the revision to which commits shall be reverted. The contents of this revision will be kept in
     *          the repository.<br>
     *          This revision must be older than the one specified in {@link #fromRevision(String)}. {@link ScmProvider}
     *          implementations may throw an exception when violating this constraint.
     * @return the builder itself.
     */
    public Builder toRevision(String revision) {
      this.request.toRevision = revision;
      return this;
    }

    /**
     * @param revision the revision from which to start reverting commits. All commits are reverted starting from this
     *          one until the revision specified in {@link #toRevision(String)}.<br>
     *          This revision must be newer than the one specified in {@link #toRevision(String)}. {@link ScmProvider}
     *          implementations may throw an exception when violating this constraint.
     * @return the builder itself.
     */
    public Builder fromRevision(String revision) {
      this.request.fromRevision = revision;
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
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for merge conflicts during the reversion.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for merge conflicts during the reversion.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for merge conflicts during the reversion.<br>
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
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for merge conflicts during the reversion.<br>
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
     * Sets a specific merge strategy for merge conflicts during the reversion.
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
     * Checks the settings of the request to build and builds the actual update request.
     *
     * @return the request for updating the local working copy.
     */
    public RevertCommitsRequest build() {
      Preconditions.checkState(this.request.fromRevision != null,
          "No from revision set (start revision of the reversion)!");
      Preconditions.checkState(this.request.toRevision != null,
          "No to revision set (end revision of the reversion that will be kept)!");
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
