package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.unleash.util.PomUtil;

/**
 * A function to convert a {@link Project} to a {@link ArtifactCoordinates} object.<br>
 * There are several implementations that include different properties of the pom and may set defaults.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public enum ProjectToCoordinates implements Function<MavenProject, ArtifactCoordinates> {
  /**
   * Uses all relevant properties of the project.
   */
  INSTANCE(true, null),
  /**
   * Assumes the default empty version for the project.
   */
  EMPTY_VERSION(false, null),
  /**
   * Uses the relevant properties of the project but assumes "pom" packaging.
   */
  POM(true, PomUtil.ARTIFACT_TYPE_POM),
  /**
   * Assumes an empty project version and packaging type "pom".
   */
  EMPTY_VERSION_POM(false, PomUtil.ARTIFACT_TYPE_POM);

  private boolean includeVersion;
  private String type;

  private ProjectToCoordinates(boolean version, String type) {
    this.includeVersion = version;
    this.type = type;
  }

  @Override
  public ArtifactCoordinates apply(MavenProject p) {
    if (this.includeVersion) {
      return new ArtifactCoordinates(p.getGroupId(), p.getArtifactId(), p.getVersion(),
          this.type != null ? this.type : p.getPackaging());
    } else {
      return new ArtifactCoordinates(p.getGroupId(), p.getArtifactId(), MavenProject.EMPTY_PROJECT_VERSION,
          this.type != null ? this.type : p.getPackaging());
    }
  }
}
