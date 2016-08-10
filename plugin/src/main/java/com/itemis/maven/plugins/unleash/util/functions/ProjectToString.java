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
  /**
   * The default instance which prints the groupId, artifactId and the version.
   */
  INSTANCE(false, true),
  /**
   * An instance which uses the groupId, the artifactId, the version and the packaging of the artifact.
   */
  INCLUDE_PACKAGING(true, true),
  /**
   * An instance which only prints the groupId and the artifactId.
   */
  EXCLUDE_VERSION(false, false);

  private boolean includePackaging;
  private boolean includeVersion;

  private ProjectToString(boolean includePackaging, boolean includeVersion) {
    this.includePackaging = includePackaging;
    this.includeVersion = includeVersion;
  }

  @Override
  public String apply(MavenProject p) {
    StringBuilder sb = new StringBuilder(p.getGroupId());
    sb.append(":").append(p.getArtifactId());
    if (this.includePackaging && p.getPackaging() != null) {
      sb.append(":").append(p.getPackaging());
    }
    if (this.includeVersion && p.getVersion() != null) {
      sb.append(":").append(p.getVersion());
    }
    return sb.toString();
  }
}
