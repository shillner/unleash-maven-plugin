package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Provides some utility methods that are necessary to prepare the release process, such as version or tag name
 * calculation.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public final class ReleaseUtil {

  /**
   * Calculates the release version depending on several strategies such as prompting the user or applying a default
   * version.
   *
   * @param version the initial version from which the release version shall be derived.
   * @param defaultReleaseVersion the default release version that should be taken into account.
   * @param prompter a {@link Prompter} for prompting the user for a release version.
   * @return the release version derived after applying several calculation strategies.
   */
  public static String getReleaseVersion(String version, Optional<String> defaultReleaseVersion,
      Optional<Prompter> prompter) {
    if (defaultReleaseVersion.isPresent()) {
      return defaultReleaseVersion.get();
    }

    String releaseVersion = MavenVersionUtil.calculateReleaseVersion(version);
    if (prompter.isPresent()) {
      try {
        releaseVersion = prompter.get().prompt("Please specify the release version", releaseVersion);
      } catch (PrompterException e) {
        // in case of an error the calculated version is used
      }
    }

    return releaseVersion;
  }

  /**
   * Calculates the next development version depending on several strategies such as prompting the user or applying a
   * default
   * version.
   *
   * @param version the initial version from which the development version shall be derived.
   * @param defaultDevelopmentVersion the default development version that should be taken into account.
   * @param prompter a {@link Prompter} for prompting the user for a version.
   * @return the development version derived after applying several calculation strategies.
   */
  public static String getNextDevelopmentVersion(String version, Optional<String> defaultDevelopmentVersion,
      Optional<Prompter> prompter) {
    if (defaultDevelopmentVersion.isPresent()) {
      return defaultDevelopmentVersion.get();
    }

    String devVersion = MavenVersionUtil.calculateNextSnapshotVersion(version);
    if (prompter.isPresent()) {
      try {
        devVersion = prompter.get().prompt("Please specify the next development version", devVersion);
      } catch (PrompterException e) {
        // in case of an error the calculated version is used
      }
    }

    return devVersion;
  }

  /**
   * Calculates an SCM tag name based on a pattern. This pattern can include the following variables that get expanded
   * during calculation:
   * <table border="1" rules="all">
   * <tr>
   * <th>variable</th>
   * <th>replaced by</th>
   * </tr>
   * <tr>
   * <td>@{project.groupId}</td>
   * <td>The groupId of the maven project.</td>
   * </tr>
   * <tr>
   * <td>@{project.artifactId}</td>
   * <td>The artifactId of the maven project.</td>
   * </tr>
   * <tr>
   * <td>@{project.version}</td>
   * <td>The version of the maven project.</td>
   * </tr>
   * </table>
   *
   * @param pattern the pattern for the tag name which may contain variables listed above.
   * @param project the project which is used for variable expansion.
   * @return the name of the tag derived from the pattern.
   */
  public static String getTagName(String pattern, MavenProject project) {
    Preconditions.checkArgument(pattern != null, "Need a tag name pattern to calculate the tag name.");
    Preconditions.checkArgument(project != null, "Need a maven project to calculate the tag name.");

    StringBuilder sb = new StringBuilder(pattern);
    int start = -1;
    while ((start = sb.indexOf("@{")) > -1) {
      int end = sb.indexOf("}");
      String var = sb.substring(start + 2, end);
      sb.replace(start, end + 1, getMavenProperty(var, project));
    }
    return sb.toString();
  }

  private static String getMavenProperty(String varName, MavenProject project) {
    if (Objects.equal("project.version", varName)) {
      return MavenVersionUtil.calculateReleaseVersion(project.getVersion());
    } else if (Objects.equal("project.artifactId", varName)) {
      return project.getArtifactId();
    } else if (Objects.equal("project.groupId", varName)) {
      return project.getGroupId();
    }
    return "";
  }

  /**
   * @return {@code true} if the environmen variable {@code UNLEASH_IT} is set to {@code true}.
   */
  public static boolean isIntegrationtest() {
    return Boolean.valueOf(System.getenv("UNLEASH_IT"));
  }
}
