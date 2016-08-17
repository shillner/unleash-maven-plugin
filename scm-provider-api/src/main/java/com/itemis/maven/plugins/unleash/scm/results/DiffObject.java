package com.itemis.maven.plugins.unleash.scm.results;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Represents the changes of a single repository object.<br>
 * The status information of such a diff object is guaranteed to be present but the textual diff may be absent if it
 * hasn't been requested.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.3.0
 */
public class DiffObject {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private String rawChangeType;
  private String oldPath;
  private String newPath;
  private String textualDiff;

  private DiffObject() {
    // use builder!
  }

  public ChangeType getChangeType() {
    return this.changeType;
  }

  public String getOldPath() {
    return this.oldPath;
  }

  public String getNewPath() {
    return this.newPath;
  }

  public Optional<String> getTextualDiff() {
    return Optional.fromNullable(this.textualDiff);
  }

  public Optional<String> getRawChangeType() {
    return Optional.fromNullable(this.rawChangeType);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.changeType, this.oldPath, this.newPath, this.textualDiff);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    DiffObject otherDiff = (DiffObject) other;
    if (Objects.equal(otherDiff.changeType, this.changeType) && Objects.equal(otherDiff.oldPath, this.oldPath)
        && Objects.equal(otherDiff.newPath, this.newPath)) {
      return Objects.equal(otherDiff.textualDiff, this.textualDiff);
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SCM Diff [").append(this.changeType).append(": ");
    switch (this.changeType) {
      case ADDED:
        sb.append(this.newPath);
        break;
      case COPIED:
        sb.append(this.oldPath).append(" => ").append(this.newPath);
        break;
      case DELETED:
        sb.append(this.oldPath);
        break;
      case MODIFIED:
        sb.append(this.newPath);
        break;
      case MOVED:
        sb.append(this.oldPath).append(" => ").append(this.newPath);
        break;
      case UNKNOWN:
        if (this.rawChangeType != null) {
          sb.insert(sb.length() - 2, "(" + this.rawChangeType + ")");
        }
        sb.append(this.oldPath).append(", ").append(this.newPath);
        break;
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.3.0
   */
  public static class Builder {
    private DiffObject diff;

    public Builder() {
      this.diff = new DiffObject();
    }

    public Builder addition(String path) {
      this.diff.changeType = ChangeType.ADDED;
      this.diff.oldPath = null;
      this.diff.newPath = path;
      return this;
    }

    public Builder deletion(String path) {
      this.diff.changeType = ChangeType.DELETED;
      this.diff.oldPath = path;
      this.diff.newPath = null;
      return this;
    }

    public Builder changed(String path) {
      this.diff.changeType = ChangeType.MODIFIED;
      this.diff.oldPath = path;
      this.diff.newPath = path;
      return this;
    }

    public Builder moved(String oldPath, String newPath) {
      this.diff.changeType = ChangeType.MOVED;
      this.diff.oldPath = oldPath;
      this.diff.newPath = newPath;
      return this;
    }

    public Builder copied(String oldPath, String newPath) {
      this.diff.changeType = ChangeType.COPIED;
      this.diff.oldPath = oldPath;
      this.diff.newPath = newPath;
      return this;
    }

    public Builder unknown(String oldPath, String newPath, String rawType) {
      this.diff.changeType = ChangeType.UNKNOWN;
      this.diff.oldPath = oldPath;
      this.diff.newPath = newPath;
      this.diff.rawChangeType = rawType;
      return this;
    }

    public Builder addTextualDiff(String textualDiff) {
      this.diff.textualDiff = textualDiff;
      return this;
    }

    public DiffObject build() {
      return this.diff;
    }
  }

  /**
   * An abstraction of the possible change types which can occur in the various repositories.
   *
   * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
   * @since 1.3.0
   */
  public static enum ChangeType {
    /**
     * The file has been added to the repository.
     */
    ADDED,
    /**
     * An existing file has been copied to another location. The original file is still present.
     */
    COPIED,
    /**
     * An existing file has been deleted from the repository.
     */
    DELETED,
    /**
     * An existing file has been modified.
     */
    MODIFIED,
    /**
     * An existing file has been moved to a new location.
     */
    MOVED,
    /**
     * An unknown change type indicates that the underlying SCM provided either no type or a change type that could not
     * be mapped to the known operations.
     */
    UNKNOWN;
  }
}