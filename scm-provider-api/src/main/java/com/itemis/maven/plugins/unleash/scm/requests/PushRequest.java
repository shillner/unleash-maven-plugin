package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

/**
 * A Request for for pushing local changes to a remote repository. This request is interesting for distributed SCMs
 * only.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Push all changes of the working copy.</li>
 * <li>Specify a merge strategy and optionally a {@link MergeClient} for updates prior to the push.</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.2.0
 */
public class PushRequest {
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  private PushRequest() {
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
  }

  /**
   * The builder for a {@link PushRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.2.0
   */
  public static class Builder {
    private PushRequest request = new PushRequest();

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for updates prior to the push.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for updates prior to the push.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for updates prior to the push.<br>
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
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for updates prior to the push.<br>
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
     * Sets a specific merge strategy for merge conflicts during updates prior to the push.
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
     * Checks the settings of the request to build and builds the actual commit request.
     *
     * @return the request for committing local changes.
     */
    public PushRequest build() {
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
