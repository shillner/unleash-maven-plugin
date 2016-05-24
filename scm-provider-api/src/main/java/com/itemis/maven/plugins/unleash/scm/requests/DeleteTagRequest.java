package com.itemis.maven.plugins.unleash.scm.requests;

public class DeleteTagRequest {
  protected String message;
  protected boolean push;
  protected String tagName;

  DeleteTagRequest() {
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

  public static class Builder {
    private DeleteTagRequest request = new DeleteTagRequest();

    public Builder message(String message) {
      this.request.message = message;
      return this;
    }

    public Builder push() {
      this.request.push = true;
      return this;
    }

    public Builder tagName(String tagName) {
      this.request.tagName = tagName;
      return this;
    }

    public DeleteTagRequest build() {
      return this.request;
    }
  }
}
