package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.merge.MergeStrategy;

public class CommitRequest {
  private String message;
  private boolean push;
  private Set<String> pathsToCommit;
  private MergeStrategy mergeStrategy = MergeStrategy.DO_NOT_MERGE;
  private MergeClient mergeClient;

  CommitRequest() {
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

  public static class Builder {
    private CommitRequest request = new CommitRequest();

    public Builder message(String message) {
      this.request.message = message;
      return this;
    }

    public Builder push() {
      this.request.push = true;
      return this;
    }

    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToCommit.add(path);
      }
      return this;
    }

    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToCommit = paths;
      } else {
        this.request.pathsToCommit = Sets.newHashSet();
      }
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

    public CommitRequest build() {
      return this.request;
    }
  }
}
