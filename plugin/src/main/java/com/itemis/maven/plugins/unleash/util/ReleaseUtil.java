package com.itemis.maven.plugins.unleash.util;

import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * Provides some utility methods that are necessary to prepare the release process, such as version or tag name
 * calculation.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@SuppressWarnings("ALL")
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
   * @param upgradeStrategy the strategy which determines the version segment to increase.
   * @return the development version derived after applying several calculation strategies.
   */
  public static String getNextDevelopmentVersion(String version, Optional<String> defaultDevelopmentVersion,
      Optional<Prompter> prompter, VersionUpgradeStrategy upgradeStrategy) {
    if (defaultDevelopmentVersion.isPresent()) {
      return defaultDevelopmentVersion.get();
    }

    String devVersion = MavenVersionUtil.calculateNextSnapshotVersion(version, upgradeStrategy);
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
   * Calculates an SCM tag name based on a pattern. This pattern can include every parameter reference that can be
   * resolved by <a href=
   * "https://maven.apache.org/ref/3.3.9/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html">PluginParameterExpressionEvaluator</a>.
   *
   * @param pattern the pattern for the tag name which may contain variables listed above.
   * @param project the Maven project to be used for version calculation during parameter resolution.
   * @param evaluator the Maven plugin parameter expression evaluator used to evaluate expressions containing parameter
   *          references.
   * @return the name of the tag derived from the pattern.
   */
  public static String getTagName(String pattern, MavenProject project, PluginParameterExpressionEvaluator evaluator) {
    Preconditions.checkArgument(pattern != null, "Need a tag name pattern to calculate the tag name.");
    Preconditions.checkArgument(evaluator != null, "Need an expression evaluator to calculate the tag name.");

    try {
      StringBuilder sb = new StringBuilder(pattern);
      int start = -1;
      while ((start = sb.indexOf("@{")) > -1) {
        int end = sb.indexOf("}");
        String var = sb.substring(start + 2, end);
        String resolved;
        // the parameter project.version gets a special treatment and will not be resolved by the evaluator but gets the
        // release version instead
        if (Objects.equal("project.version", var)) {
          resolved = MavenVersionUtil.calculateReleaseVersion(project.getVersion());
        } else {
          String expression = "${" + var + "}";
          resolved = evaluator.evaluate(expression).toString();
        }
        sb.replace(start, end + 1, resolved);
      }
      return sb.toString();
    } catch (ExpressionEvaluationException e) {
      throw new RuntimeException("Could not resolve expressions in pattern: " + pattern, e);
    }
  }

  /**
   * @return {@code true} if the environmen variable {@code UNLEASH_IT} is set to {@code true}.
   */
  public static boolean isIntegrationtest() {
    return Boolean.valueOf(System.getenv("UNLEASH_IT")) || Boolean.valueOf(System.getProperty("unleash.it"));
  }

  /**
   * Determines the home directory of the maven installation to be used.
   *
   * @param userDefined an optional user-defined path to use as maven home.
   * @return a path to a maven installation or {@code null} if none could be determined.
   */
  public static File getMavenHome(java.util.Optional<String> userDefined) {
    String path = null;
    if (isValidMavenHome(userDefined.orElse(null))) {
      path = userDefined.orElse(null);
    } else {
      String sysProp = System.getProperty("maven.home");
      if (isValidMavenHome(sysProp)) {
        path = sysProp;
      } else {
        String envVar = System.getenv("M2_HOME");
        if (isValidMavenHome(envVar)) {
          path = envVar;
        }
      }
    }
    if (path != null) {
      return new File(path);
    }
    return null;
  }

  private static boolean isValidMavenHome(String path) {
    if (path != null) {
      File homeFolder = new File(path);
      return homeFolder.exists() && homeFolder.isDirectory() && new File(homeFolder, "bin/mvn").exists();
    }
    return false;
  }
}
