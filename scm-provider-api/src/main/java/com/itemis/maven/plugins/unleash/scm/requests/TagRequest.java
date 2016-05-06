package com.itemis.maven.plugins.unleash.scm.requests;

import com.google.common.base.Optional;

public class TagRequest {
  protected String message;
  protected boolean push;
  protected String revision;
  protected String tagName;
  protected boolean commitBeforeTagging;

  TagRequest() {
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

  public String getTagName() {
    return this.tagName;
  }

  public Optional<String> getRevision() {
    return Optional.fromNullable(this.revision);
  }

  public boolean useWorkingCopy() {
    return this.revision == null;
  }

  public boolean commitBeforeTagging() {
    return this.commitBeforeTagging;
  }

  public static class Builder {
    private TagRequest request = new TagRequest();

    public Builder setMessage(String message) {
      this.request.message = message;
      return this;
    }

    public Builder push() {
      this.request.push = true;
      return this;
    }

    public Builder setTagName(String tagName) {
      this.request.tagName = tagName;
      return this;
    }

    public Builder setRevision(String revision) {
      this.request.revision = revision;
      return this;
    }

    public Builder commitBeforeTagging() {
      this.request.commitBeforeTagging = true;
      return this;
    }

    public TagRequest build() {
      return this.request;
    }
  }
}
