package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;

public enum ProjectToString implements Function<MavenProject, String> {
  INSTANCE;

  @Override
  public String apply(MavenProject p) {
    StringBuilder sb = new StringBuilder(p.getGroupId());
    sb.append(":").append(p.getArtifactId()).append(":").append(p.getVersion());
    return sb.toString();
  }
}
