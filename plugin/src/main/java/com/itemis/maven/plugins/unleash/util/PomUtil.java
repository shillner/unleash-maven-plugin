package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.project.MavenProject;

/**
 * Some common constants regarding the POM.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public final class PomUtil {
  private PomUtil() {
    // Should not be instanciated
  }

  public static final String VERSION_QUALIFIER_SNAPSHOT = "-SNAPSHOT";

  public static final String ARTIFACT_TYPE_POM = "pom";
  public static final String ARTIFACT_TYPE_JAR = "jar";

  public static final String getBasicCoordinates(MavenProject project) {
    StringBuilder sb = new StringBuilder(project.getGroupId()).append(':').append(project.getArtifactId()).append(':')
        .append(project.getVersion());
    return sb.toString();
  }
}
