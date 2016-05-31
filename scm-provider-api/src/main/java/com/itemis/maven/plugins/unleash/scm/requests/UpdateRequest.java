package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

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

  public static class Builder {
    private UpdateRequest request = new UpdateRequest();

    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToUpdate.add(path);
      }
      return this;
    }

    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToUpdate = paths;
      } else {
        this.request.pathsToUpdate = Sets.newHashSet();
      }
      return this;
    }

    public Builder toRevision(String revision) {
      this.request.targetRevision = revision;
      return this;
    }

    public Builder mergeUseLocal() {
      this.request.mergeStrategy = MergeStrategy.USE_LOCAL;
      return this;
    }

    public Builder mergeUseRemote() {
      this.request.mergeStrategy = MergeStrategy.USE_REMOTE;
      return this;
    }

    public Builder merge() {
      this.request.mergeStrategy = MergeStrategy.FULL_MERGE;
      return this;
    }

    public Builder noMerge() {
      this.request.mergeStrategy = MergeStrategy.DO_NOT_MERGE;
      return this;
    }

    public Builder mergeStrategy(MergeStrategy mergeStrategy) {
      if (mergeStrategy != null) {
        this.request.mergeStrategy = mergeStrategy;
      } else {
        noMerge();
      }
      return this;
    }

    public Builder mergeClient(MergeClient mergeClient) {
      this.request.mergeClient = mergeClient;
      return this;
    }

    public UpdateRequest build() {
      return this.request;
    }
  }
}
