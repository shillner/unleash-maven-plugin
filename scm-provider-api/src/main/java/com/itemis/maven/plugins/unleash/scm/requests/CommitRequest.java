package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class CommitRequest {
  protected String message;
  protected boolean push;
  protected Set<String> pathsToCommit;

  CommitRequest() {
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

  public Optional<Set<String>> getPathsToCommit() {
    return Optional.fromNullable(this.pathsToCommit);
  }

  public boolean commitAllChanges() {
    return this.pathsToCommit == null;
  }

  public static class Builder {
    private CommitRequest request = new CommitRequest();

    public Builder setMessage(String message) {
      this.request.message = message;
      return this;
    }

    public Builder push() {
      this.request.push = true;
      return this;
    }

    public Builder addPaths(String... paths) {
      if (paths.length > 0) {
        if (this.request.pathsToCommit == null) {
          this.request.pathsToCommit = Sets.newHashSet();
        }

        for (String path : paths) {
          this.request.pathsToCommit.add(path);
        }
      }
      return this;
    }

    public Builder setPaths(Set<String> paths) {
      this.request.pathsToCommit = paths;
      return this;
    }

    public CommitRequest build() {
      return this.request;
    }
  }
}
