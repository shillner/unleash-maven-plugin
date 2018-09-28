package com.itemis.maven.plugins.unleash.util;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Splitter;

public class Repository {
  @Parameter(required = true)
  private String id;
  @Parameter(required = true)
  private String url;

  // only for local usage during property parsing
  private boolean releasesEnabled = true;

  public String getId() {
    return this.id;
  }

  public String getUrl() {
    return this.url;
  }

  public static Optional<Repository> parseFromProperty(String value) {
    Repository repo = new Repository();

    Splitter.on(',').split(value).forEach(s -> {
      int i = s.indexOf('=');
      if (i > 0 && i < s.length() - 1) {
        String k = s.substring(0, i).trim();
        String v = s.substring(i + 1, s.length()).trim();
        switch (k) {
          case "id":
            repo.id = v;
            break;
          case "url":
            repo.url = v;
            break;
          case "releases":
            repo.releasesEnabled = Boolean.parseBoolean(v);
            break;
        }
      }
    });

    if (repo.releasesEnabled && StringUtils.isNotBlank(repo.id) && StringUtils.isNoneBlank(repo.url)) {
      return Optional.of(repo);
    }
    return Optional.empty();
  }
}
