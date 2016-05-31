package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

/**
 * A Request for committing changes of the local working copy to the repository (local only or local and remote for
 * distributed SCMs).<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Commit all changes of the working copy.</li>
 * <li>Commit a set of files only.</li>
 * <li>Specify a merge strategy and optionally a {@link MergeClient} for updates prior to the commit.</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public class CommitRequest {
  private String message;
  private boolean push;
  private Set<String> pathsToCommit;
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  private CommitRequest() {
    this.pathsToCommit = Sets.newHashSet();
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

  public Set<String> getPathsToCommit() {
    return this.pathsToCommit;
  }

  public boolean commitAllChanges() {
    return this.pathsToCommit.isEmpty();
  }

  public MergeStrategy getMergeStrategy() {
    return this.mergeStrategy;
  }

  public Optional<MergeClient> getMergeClient() {
    return Optional.fromNullable(this.mergeClient);
  }

  /**
   * The builder for a {@link CommitRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private CommitRequest request = new CommitRequest();

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
     * Adds some working directory-relative paths of files or folders to the list of paths to commit.<br>
     * Once some paths are added only these files are committed, nothing else!<br>
     * You can use {@link #paths(null)} to unset the list of files and commit all changes of the working copy.
     *
     * @param paths some filepaths to commit.
     * @return the builder itself.
     */
    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToCommit.add(path);
      }
      return this;
    }

    /**
     * Sets the working directory-relative paths of files or folders to commit. This method totally overrides all paths
     * added previously!<br>
     * Once some paths are added only these files are committed, nothing else!<br>
     * Use {@link #paths(null)} to unset the list of files and commit all changes of the working copy.
     *
     * @param paths the filepaths to commit.
     * @return the builder itself.
     */
    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToCommit = paths;
      } else {
        this.request.pathsToCommit = Sets.newHashSet();
      }
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_LOCAL} for updates prior to the commit.<br>
     * This will request overriding of all conflicting changes with the local versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#USE_REMOTE} for updates prior to the commit.<br>
     * This will request overriding of all conflicting changes with the remote versions.
     *
     * @return the builder itself.
     */
    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    /**
     * Sets the merge strategy to {@link MergeStrategy#FULL_MERGE} for updates prior to the commit.<br>
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
     * Sets the merge strategy to {@link MergeStrategy#DO_NOT_MERGE} for updates prior to the commit.<br>
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
     * Checks the settings of the request to build and builds the actual commit request.
     *
     * @return the request for committing local changes.
     */
    public CommitRequest build() {
      Preconditions.checkState(this.request.message != null, "No log message specified!");
      if (MergeStrategy.FULL_MERGE == this.request.mergeStrategy) {
        Preconditions.checkState(this.request.mergeClient != null,
            "Merge strategy " + this.request.mergeStrategy + " has been requested but no merge client is set!");
      }
      return this.request;
    }
  }
}
