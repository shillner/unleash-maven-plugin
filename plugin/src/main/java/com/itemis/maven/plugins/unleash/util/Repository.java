package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.plugins.annotations.Parameter;

public class Repository {
  @Parameter(required = true)
  private String id;
  @Parameter(required = true)
  private String url;

  public String getId() {
    return this.id;
  }

  public String getUrl() {
    return this.url;
  }
}
