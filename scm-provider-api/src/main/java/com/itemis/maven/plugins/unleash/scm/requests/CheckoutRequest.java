package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * A Request for the checking out a remote repository into the local working directory. In case of distributed SCMs this
 * is equal to cloning the repository.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Specify a repository base URL in conjunction with a branch or tag name.</li>
 * <li>Specify the full repository URL including the branch or tag name if this information can be included in the URL.
 * </li>
 * <li>Checkout only a few files from the repository by setting the repository-relative paths of these files or
 * directories.</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
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

  /**
   * The builder for a {@link CheckoutRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 0.1.0
   */
  public static class Builder {
    private CheckoutRequest request = new CheckoutRequest();

    /**
     * @param remoteRepositoryUrl the URL of the remote repository to check out (mandatory).
     * @return the builder itself.
     */
    public Builder from(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    /**
     * @param revision the revision of the repository branch to checkout. If this revision is omitted, HEAD is assumed.
     * @return the builder itself.
     */
    public Builder revision(String revision) {
      this.request.revision = revision;
      return this;
    }

    /**
     * Sets the name of the branch from which the SCM provider shall checkout.<br>
     * Setting this parameter makes only sense if the remote URL links to the repository base, not a specific branch.
     * <br>
     * <br>
     * This parameter excludes the usage of {@link #tag(String)} and unsets the tag parameter value.
     *
     * @param branchName the name of the branch to checkout.
     * @return the builder itself.
     */
    public Builder branch(String branchName) {
      this.request.branch = branchName;
      this.request.tag = null;
      return this;
    }

    /**
     * Sets the name of the tag from which the SCM provider shall checkout.<br>
     * Setting this parameter makes only sense if the remote URL links to the repository base, not a specific branch.
     * <br>
     * <br>
     * This parameter excludes the usage of {@link #branch(String)} and unsets the branch parameter value.
     *
     * @param tagName the name of the tag to checkout.
     * @return the builder itself.
     */
    public Builder tag(String tagName) {
      this.request.tag = tagName;
      this.request.branch = null;
      return this;
    }

    /**
     * Adds some repository-relative paths of files or folders to the list of paths to checkout.<br>
     * Once some paths are added only these files are checked out, nothing else!<br>
     * You can use {@link #paths(null)} to unset the list of files and checkout the whole repository.
     *
     * @param paths some filepaths to checkout.
     * @return the builder itself.
     */
    public Builder addPaths(String... paths) {
      for (String path : paths) {
        this.request.pathsToCheckout.add(path);
      }
      return this;
    }

    /**
     * Sets the repository-relative paths of files or folders to checkout. This method totally overrides all paths added
     * previously!<br>
     * Once some paths are added only these files are checked out, nothing else!<br>
     * Use {@link #paths(null)} to unset the list of files and checkout the whole repository.
     *
     * @param paths the filepaths to checkout.
     * @return the builder itself.
     */
    public Builder paths(Set<String> paths) {
      if (paths != null) {
        this.request.pathsToCheckout = paths;
      } else {
        this.request.pathsToCheckout = Sets.newHashSet();
      }
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual checkout request.
     *
     * @return the request for checking out the repository.
     */
    public CheckoutRequest build() {
      Preconditions.checkState(this.request.getRemoteRepositoryUrl() != null, "No remote repository URL specified!");
      return this.request;
    }
  }
}
