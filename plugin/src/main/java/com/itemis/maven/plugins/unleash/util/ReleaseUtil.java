package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public final class ReleaseUtil {
  // TODO need to handle reactors where modules have different versions!
  public static String getReleaseVersion(String version, String defaultReleaseVersion) {
    if (!Strings.isNullOrEmpty(defaultReleaseVersion)) {
      return defaultReleaseVersion;
    }

    // TODO calc for each project
    // in interactive mode, ask user whether versions are correct
    return MavenVersionUtil.calculateReleaseVersion(version);
  }

  public static String getNextDevelopmentVersion(String version, String defaultDevelopmentVersion) {
    if (!Strings.isNullOrEmpty(defaultDevelopmentVersion)) {
      return defaultDevelopmentVersion;
    }

    return MavenVersionUtil.calculateNextSnapshotVersion(version);
  }

  // supports @{project.version}, @{project.artifactId}, @{project.groupId}
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

  public static boolean isIntegrationtest() {
    return Boolean.valueOf(System.getenv("unleash.it"));
  }
}
