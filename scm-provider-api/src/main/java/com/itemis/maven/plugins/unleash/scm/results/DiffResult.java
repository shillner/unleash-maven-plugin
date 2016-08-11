package com.itemis.maven.plugins.unleash.scm.results;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * A result object containing a set of {@link DiffObject DiffObjects}, one for each changed repository object.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.3.0
 */
public class DiffResult {
  private Set<DiffObject> diffs;

  private DiffResult() {
    this.diffs = Sets.newHashSet();
    // use builder!
  }

  public static Builder builder() {
    return new Builder();
  }

  public Set<DiffObject> get() {
    return Collections.unmodifiableSet(this.diffs);
  }

  /**
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.3.0
   */
  public static class Builder {
    private DiffResult result;

    private Builder() {
      this.result = new DiffResult();
    }

    /**
     * Adds the given diff to the result object.
     *
     * @param diff the diff object to be added.
     * @return the builder itself.
     */
    public Builder addDiff(DiffObject diff) {
      this.result.diffs.add(diff);
      return this;
    }

    /**
     * @return an immutable diff collection.
     */
    public DiffResult build() {
      return this.result;
    }
  }
}
