package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;

public enum ProjectToString implements Function<MavenProject, String> {
  INSTANCE(false), INCLUDE_PACKAGING(true);

  private boolean includePackaging;

  private ProjectToString(boolean includePackaging) {
    this.includePackaging = includePackaging;
  }

  @Override
  public String apply(MavenProject p) {
    StringBuilder sb = new StringBuilder(p.getGroupId());
    sb.append(":").append(p.getArtifactId());
    if (this.includePackaging && p.getPackaging() != null) {
      sb.append(":").append(p.getPackaging());
    }
    if (p.getVersion() != null) {
      sb.append(":").append(p.getVersion());
    }
    return sb.toString();
  }
}
