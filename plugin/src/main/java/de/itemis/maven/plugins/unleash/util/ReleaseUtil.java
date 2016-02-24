package de.itemis.maven.plugins.unleash.util;

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

  public static String getNextDevelopmentVersion(String version, String defaultDevelopmentversion) {
    if (!Strings.isNullOrEmpty(defaultDevelopmentversion)) {
      return defaultDevelopmentversion;
    }

    return MavenVersionUtil.calculateNextSnapshotVersion(version);
  }
}
