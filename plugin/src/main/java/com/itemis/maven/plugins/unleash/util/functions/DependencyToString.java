package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Dependency;

import com.google.common.base.Function;

public enum DependencyToString implements Function<Dependency, String> {
  INSTANCE;

  @Override
  public String apply(Dependency d) {
    StringBuilder sb = new StringBuilder(d.getGroupId());
    sb.append(":").append(d.getArtifactId());
    if (d.getType() != null) {
      sb.append(":").append(d.getType());
    }
    if (d.getClassifier() != null) {
      sb.append(":").append(d.getClassifier());
    }
    sb.append(":").append(d.getVersion());
    return sb.toString();
  }
}
