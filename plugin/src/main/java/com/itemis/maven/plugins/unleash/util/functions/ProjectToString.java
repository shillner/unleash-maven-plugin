package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;

/**
 * A function to convert a {@link Project} to its String representation in coordinates format.<br>
 * There are two implementations that either include or exclude the project packaging.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
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
