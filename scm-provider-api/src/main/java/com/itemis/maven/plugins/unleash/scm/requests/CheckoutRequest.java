package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

public class CheckoutRequest {
  private String remoteRepositoryUrl;
  private String revision;
  private String branch;
  private String tag;
  private Set<String> pathsToCheckout;

  private CheckoutRequest() {
    this.pathsToCheckout = Sets.newHashSet();
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getRemoteRepositoryUrl() {
    return this.remoteRepositoryUrl;
  }

  public Optional<String> getRevision() {
    return Optional.fromNullable(this.revision);
  }

  public Optional<String> getBranch() {
    return Optional.fromNullable(this.branch);
  }

  public Optional<String> getTag() {
    return Optional.fromNullable(this.tag);
  }

  public Set<String> getPathsToCheckout() {
    return this.pathsToCheckout;
  }

  public boolean checkoutWholeRepository() {
    return this.pathsToCheckout.isEmpty();
  }

  public boolean checkoutBranch() {
    return this.branch != null;
  }

  public boolean checkoutTag() {
    return this.tag != null;
  }

  public static class Builder {
    private CheckoutRequest request = new CheckoutRequest();

    public Builder from(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    public Builder revision(String revision) {
      this.request.revision = revision;
      return this;
    }

    public Builder branch(String branchName) {
      this.request.branch = branchName;
      this.request.tag = null;
      return this;
    }

    public Builder tag(String tagName) {
      this.request.tag = tagName;
      this.request.branch = null;
      return this;
    }

    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToCheckout.add(path);
      }
      return this;
    }

    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToCheckout = paths;
      } else {
        this.request.pathsToCheckout = Sets.newHashSet();
      }
      return this;
    }

    public CheckoutRequest build() {
      Preconditions.checkState(this.request.getRemoteRepositoryUrl() != null, "No remote repository URL specified!");
      return this.request;
    }
  }
}
