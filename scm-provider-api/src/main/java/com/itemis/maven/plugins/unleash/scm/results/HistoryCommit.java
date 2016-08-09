package com.itemis.maven.plugins.unleash.scm.results;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Represents a commit object in the commit history of the repository.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.2.0
 */
public class HistoryCommit {
  private String revision;
  private String message;
  private String author;
  private Date date;

  private HistoryCommit() {
    // use builder!
  }

  /**
   * @return the revision of this commit which can be understood as its unique id. This can be the revision number of
   *         SVN or the ObjectIds of Git.
   */
  public String getRevision() {
    return this.revision;
  }

  /**
   * @return the commit message or {@code null} if none has been specified.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * @return the author of the commit which can also be null.
   */
  public String getAuthor() {
    return this.author;
  }

  /**
   * @return the date when the commit happened.
   */
  public Date getDate() {
    return this.date;
  }

  /**
   * @return A builder for creating an immutable commit.
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.revision);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != HistoryCommit.class) {
      return false;
    }
    HistoryCommit otherCommit = (HistoryCommit) other;
    return otherCommit.getRevision() == this.revision;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(this.revision);
    sb.append(StringUtils.LF).append("Message: ").append(Strings.nullToEmpty(this.message));
    sb.append(StringUtils.LF).append("Author: ").append(Strings.nullToEmpty(this.author));
    sb.append(StringUtils.LF).append("Date: ").append(this.date == null ? StringUtils.EMPTY : this.date.toString());
    return sb.toString();
  }

  public static class Builder {
    private HistoryCommit commit;

    private Builder() {
      this.commit = new HistoryCommit();
    }

    /**
     * @param revision the revision of this commit which can be understood as its unique id. This can be the revision
     *          number of
     *          SVN or the ObjectIds of Git.
     * @return The builder itself.
     */
    public Builder setRevision(String revision) {
      this.commit.revision = revision;
      return this;
    }

    /**
     * @param message the human readable message of the commit.
     * @return The builder itself.
     */
    public Builder setMessage(String message) {
      this.commit.message = message;
      return this;
    }

    /**
     * @param author the author of the commit.
     * @return The builder itself.
     */
    public Builder setAuthor(String author) {
      this.commit.author = author;
      return this;
    }

    /**
     * @param date the date at which this commit has been triggered.
     * @return The builder itself.
     */
    public Builder setDate(Date date) {
      this.commit.date = date;
      return this;
    }

    /**
     * @return the commit object with all information added to the builder.
     */
    public HistoryCommit build() {
      Preconditions.checkState(StringUtils.isNotBlank(this.commit.revision),
          "A commit cannot be created without a commit id (revision).");
      return this.commit;
    }
  }
}
