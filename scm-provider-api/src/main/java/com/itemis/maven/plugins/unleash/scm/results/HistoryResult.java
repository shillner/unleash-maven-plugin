package com.itemis.maven.plugins.unleash.scm.results;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.itemis.maven.plugins.unleash.scm.requests.HistoryRequest;

/**
 * A result object containing the commit history of a repository. The default sorting of the history is always
 * DESCENDING which means that the history will start with the latest commits on top.
 * This history may not be the complete history since it matches the filters specified in the {@link HistoryRequest}.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.2.0
 */
public class HistoryResult {
  private List<HistoryCommit> commits;

  private HistoryResult() {
    this.commits = Lists.newArrayList();
    // use builder
  }

  /**
   * @return an immutable list of commits in default order (DESCENDING) starting with the most recent commit.
   */
  public List<HistoryCommit> get() {
    return Collections.unmodifiableList(this.commits);
  }

  /**
   * @return an immutable list of commits in the reverse order (ASCENDING) starting with the oldest commit.
   */
  public List<HistoryCommit> reverse() {
    return Collections.unmodifiableList(Lists.reverse(this.commits));
  }

  /**
   * @return a fresh builder for the creation of an immutable commit history.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private HistoryResult result;

    private Builder() {
      this.result = new HistoryResult();
    }

    /**
     * Adds the given commit at the end of the commit history.
     * 
     * @param commit the commit to be added to the history.
     * @return the builder itself.
     */
    public Builder addCommit(HistoryCommit commit) {
      this.result.commits.add(commit);
      return this;
    }

    /**
     * @return an immutable commit history.
     */
    public HistoryResult build() {
      return this.result;
    }
  }
}
