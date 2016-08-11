package com.itemis.maven.plugins.unleash.scm.requests;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/**
 * A Request for the creation of a commit history for the current repository (working dir) or a remote one. Note that
 * the remote repository option may not be supported by all SCM types.<br>
 * <b>USE {@link #builder()} TO CREATE A REQUEST!</b><br>
 * <br>
 * The following configuration options are possible:
 * <ol>
 * <li>Use the repository that is checked out in your current working directory for the history creation.</li>
 * <li>Specify a repository URL to query the history of a remote repository. This may not be supported by all SCMs!</li>
 * <li>Specify regular expressions to filter commit messages that are not of interest.</li>
 * <li>Use any combination of startTag/endTag and startRevision/endRevision to specify a revision range for the
 * query.</li>
 * </ol>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.2.0
 */
public class HistoryRequest {
  private String remoteRepositoryUrl;
  private String startRevision;
  private String endRevision;
  private long maxResults = -1;
  private String startTag;
  private String endTag;
  private Set<String> messageFilters;

  private HistoryRequest() {
    this.messageFilters = Sets.newHashSet();
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<String> getRemoteRepositoryUrl() {
    return Optional.fromNullable(this.remoteRepositoryUrl);
  }

  public Optional<String> getStartRevision() {
    return Optional.fromNullable(this.startRevision);
  }

  public Optional<String> getEndRevision() {
    return Optional.fromNullable(this.endRevision);
  }

  public long getMaxResults() {
    return this.maxResults;
  }

  public Optional<String> getStartTag() {
    return Optional.fromNullable(this.startTag);
  }

  public Optional<String> getEndTag() {
    return Optional.fromNullable(this.endTag);
  }

  public Set<String> getMessageFilters() {
    return Collections.unmodifiableSet(this.messageFilters);
  }

  /**
   * The builder for a {@link HistoryRequest}.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.2.0
   */
  public static class Builder {
    private HistoryRequest request = new HistoryRequest();

    /**
     * @param remoteRepositoryUrl the remote URL to retrieve the history from. If this url is omitted, the history will
     *          be retrieved locally or from the remote repository from which the local working directory has been
     *          cloned. Note that some SCM providers may not support history retrieval from remote repositories. In this
     *          case those implementations may throw an exception.
     * @return the builder itself.
     */
    public Builder fromRemote(String remoteRepositoryUrl) {
      this.request.remoteRepositoryUrl = remoteRepositoryUrl;
      return this;
    }

    /**
     * The revision to start the history from. This can be the SVN revision number or the SHA1 hash of a git commit.<br>
     * Note that the start revision is expected to be older than the end revision.<br>
     * Specifying a start revision will unset an already set start tag and vice versa.
     *
     * @param startRevision the revision to start the history from.
     * @return the builder itself.
     */
    public Builder startRevision(String startRevision) {
      this.request.startRevision = startRevision;
      this.request.startTag = null;
      return this;
    }

    /**
     * The revision at which the history shall end. This can be the SVN revision number or the SHA1 hash of a git
     * commit.<br>
     * Note that the end revision is expected to be younger than the start revision.<br>
     * Specifying an end revision will unset an already set end tag and vice versa.
     *
     * @param endRevision the revision at which the revision history shall end.
     * @return the builder itself.
     */
    public Builder endRevision(String endRevision) {
      this.request.endRevision = endRevision;
      this.request.endTag = null;
      return this;
    }

    /**
     * The tag name from which the history shall start. This tag is used to find the appropriate revision in the commit
     * history that can be used as the upper bound of the range.<br>
     * Note that the start tag is expected to be older than the end tag.<br>
     * Specifying a start tag will unset an already set start revision and vice versa.
     *
     * @param endRevision the tag at which the revision history shall start.
     * @return the builder itself.
     */
    public Builder startTag(String startTag) {
      this.request.startTag = startTag;
      this.request.startRevision = null;
      return this;
    }

    /**
     * The tag name at which the history shall end. This tag is used to find the appropriate revision in the commit
     * history that can be used as the lower bound of the range.<br>
     * Note that the end tag is expected to be younger than the start tag.<br>
     * Specifying an end tag will unset an already set end revision and vice versa.
     *
     * @param endRevision the tag at which the revision history shall end.
     * @return the builder itself.
     */
    public Builder endTag(String endTag) {
      this.request.endTag = endTag;
      this.request.endRevision = null;
      return this;
    }

    /**
     * @param maxResults the maximum number of results the history shall contain.
     * @return the builder itself.
     */
    public Builder maxResults(long maxResults) {
      this.request.maxResults = maxResults;
      return this;
    }

    /**
     * Adds a filter working on the commit messages. Those filters can be used to filter out commits that are not if
     * interest.
     *
     * @param filterExpression a regular expression pattern that can be used to match commit messages against.
     * @return the builder itself.
     */
    public Builder addMessageFilter(String filterExpression) {
      if (StringUtils.isNotEmpty(filterExpression)) {
        this.request.messageFilters.add(filterExpression);
      }
      return this;
    }

    /**
     * Checks the settings of the request to build and builds the actual history request.
     *
     * @return the request for retrieving the history of the repository.
     */
    public HistoryRequest build() {
      return this.request;
    }
  }
}
