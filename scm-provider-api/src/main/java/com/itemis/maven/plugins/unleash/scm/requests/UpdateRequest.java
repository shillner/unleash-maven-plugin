package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

/**
 * A Request for updating the local working copy from the remote repository.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Update the whole working directory.</li>
 * <li>Update only specific files of the working directory.</li>
 * <li>Specify a merge strategy and optionally a {@link MergeClient} for update conflicts.</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class UpdateRequest {
  private Set<String> pathsToUpdate;
  private String targetRevision;
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  UpdateRequest() {
    this.pathsToUpdate = Sets.newHashSet();
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public Set<String> getPathsToUpdate() {
    return this.pathsToUpdate;
  }

  public boolean updateAllChanges() {
    return this.pathsToUpdate.isEmpty();
  }

  public Optional<String> getTargetRevision() {
    return Optional.fromNullable(this.targetRevision);
  }

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
  }

  /**
   * The builder for a {@link UpdateRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private UpdateRequest request = new UpdateRequest();

    /**
     * Adds some working directory-relative paths of files or folders to the list of paths to update.<br>
     * Once some paths are added only these files are updated, nothing else!<br>
     * You can use {@link #paths(null)} to unset the list of files and update all changes of the working copy.
     *
     * @param paths some filepaths to update.
     * @return the builder itself.
     */
    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToUpdate.add(path);
      }
      return this;
    }

    /**
     * Sets the working directory-relative paths of files or folders to update. This method totally overrides all paths
     * added previously!<br>
     * Once some paths are added only these files are updated, nothing else!<br>
     * Use {@link #paths(null)} to unset the list of files and update all changes of the working copy.
     *
     * @param paths the filepaths to update.
     * @return the builder itself.
     */
    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToUpdate = paths;
      } else {
        this.request.pathsToUpdate = Sets.newHashSet();
      }
      return this;
    }

    /**
     * @param revision the revision to which the working copy shall be updated. If this revision is omitted, HEAD is
     *          assumed.
     * @return the builder itself.
     */
    public Builder toRevision(String revision) {
      this.request.targetRevision = revision;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for merge conflicts during the update.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for merge conflicts during the update.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for merge conflicts during the update.<br>
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
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for merge conflicts during the update.<br>
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
    public UpdateRequest build() {
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
