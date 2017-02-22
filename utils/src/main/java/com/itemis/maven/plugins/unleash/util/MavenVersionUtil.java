package com.itemis.maven.plugins.unleash.util;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * A utility class for maven version management.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public final class MavenVersionUtil {
  public static final String VERSION_QUALIFIER_SNAPSHOT = "-SNAPSHOT";
  public static final String VERSION_LATEST = "LATEST";

  private MavenVersionUtil() {
    // static utility methods only
  }

  /**
   * Checks whether the passed version represents a SNAPSHOT version by checking if the version String ends with
   * '-SNAPSHOT' or equals 'LATEST'.
   *
   * @param version the version to check
   * @return {@code true} if the version String represents a SNAPSHOT version.
   */
  public static boolean isSnapshot(String version) {
    return version.toUpperCase().endsWith(VERSION_QUALIFIER_SNAPSHOT) || version.toUpperCase().equals(VERSION_LATEST);
  }

  /**
   * Calculates the next higher SNAPSHOT version of the passed version String by incrementing the respective number
   * segment of the version String by one that is determined by the chosen upgrade strategy.<br>
   * <strong>Some examples:</strong>
   * <table style="width:50%;border:2px solid black;border-collapse:collapse;">
   * <tr>
   * <th>Input version</th>
   * <th>Result</th>
   * </tr>
   * <tr>
   * <td>1.0.0</td>
   * <td>1.0.1-SNAPSHOT</td>
   * </tr>
   * <tr>
   * <td>1.12</td>
   * <td>1.13-SNAPSHOT</td>
   * </tr>
   * <tr>
   * <td>3-SNAPSHOT</td>
   * <td>4-SNAPSHOT</td>
   * </tr>
   * <tr>
   * <td>1.3-SNAPSH</td>
   * <td>1.4-SNAPSH-SNAPSHOT</td>
   * </tr>
   * <tr>
   * <td>3-Alpha1</td>
   * <td>3-Alpha2-SNAPSHOT</td>
   * </tr>
   * <tr>
   * <td>3-Alpha1-SNAPSHOT</td>
   * <td>3-Alpha2-SNAPSHOT</td>
   * </tr>
   * </table>
   *
   * @param version the current version from which the follow-up SNAPSHOT version shall be calculated.
   * @param upgradeStrategy the strategy which determines the part of the version to upgrade.
   * @return the next higher SNAPSHOT version.
   */
  public static String calculateNextSnapshotVersion(String version, VersionUpgradeStrategy upgradeStrategy) {
    VersionUpgradeStrategy strategy = upgradeStrategy != null ? upgradeStrategy : VersionUpgradeStrategy.DEFAULT;
    Version v = Version.parse(version);
    v.increase(strategy);

    String snapshotVersion = v.toString();
    if (!snapshotVersion.endsWith(VERSION_QUALIFIER_SNAPSHOT)) {
      snapshotVersion += VERSION_QUALIFIER_SNAPSHOT;
    }
    return snapshotVersion;
  }

  /**
   * Calculates the next higher SNAPSHOT version of the passed version String by incrementing the lowest possible
   * number
   * segment of the version String by one.<br>
   * This is just a convenience method which calls {@link #calculateNextSnapshotVersion(String,
   * VersionUpgradeStrategy)}
   * with the passed version and {@link VersionUpgradeStrategy#DEFAULT}.
   *
   * @param version the current version from which the follow-up SNAPSHOT version shall be calculated.
   * @return the next higher SNAPSHOT version.
   */
  public static String calculateNextSnapshotVersion(String version) {
    return calculateNextSnapshotVersion(version, VersionUpgradeStrategy.DEFAULT);
  }

  /**
   * Calculates a release version String based on the passed version String which may (or not) be a SNAPSHOT version
   * String.<br>
   * <strong>Some examples:</strong>
   * <table style="width:50%;border:2px solid black;border-collapse:collapse;">
   * <tr>
   * <th>Input version</th>
   * <th>Result</th>
   * </tr>
   * <tr>
   * <td>1.0.0</td>
   * <td>1.0.0</td>
   * </tr>
   * <tr>
   * <td>1.12-SNAPSHOT</td>
   * <td>1.12</td>
   * </tr>
   * <tr>
   * <td>3-SNAPSHOT</td>
   * <td>3-SNAPSHOT</td>
   * </tr>
   * </table>
   *
   * @param version the current version from which the release version shall be calculated.
   * @return the calculated release version which might be identical to the passed version.
   */
  public static String calculateReleaseVersion(String version) {
    if (version.toUpperCase().endsWith(VERSION_QUALIFIER_SNAPSHOT)) {
      return version.substring(0, version.length() - VERSION_QUALIFIER_SNAPSHOT.length());
    }
    return version;
  }

  /**
   * Checks whether version1 is newer than version 2.
   *
   * @param version1 the first version to check
   * @param version2 the second version to check (the anchestor)
   * @return {@code true} if version1 is newer than version2.
   */
  public static boolean isNewerVersion(String version1, String version2) {
    String v1 = Strings.emptyToNull(version1);
    String v2 = Strings.emptyToNull(version2);

    if (Objects.equal(v1, v2)) {
      return false;
    } else if (v1 == null) {
      return false;
    } else if (v2 == null) {
      return true;
    } else if (Objects.equal(VERSION_LATEST, v1)) {
      return true;
    } else if (Objects.equal(VERSION_LATEST, v2)) {
      return false;
    }

    String commonPrefix = Strings.commonPrefix(v1, v2);
    v1 = v1.substring(commonPrefix.length());
    v2 = v2.substring(commonPrefix.length());
    String commonSuffix = Strings.commonSuffix(v1, v2);
    v1 = v1.substring(0, v1.length() - commonSuffix.length());
    v2 = v2.substring(0, v2.length() - commonSuffix.length());

    if (v1.isEmpty()) {
      if (Objects.equal(VERSION_QUALIFIER_SNAPSHOT, v2.toUpperCase())) {
        return true;
      } else {
        return false;
      }
    } else if (Objects.equal(VERSION_QUALIFIER_SNAPSHOT, v1.toUpperCase())) {
      return false;
    } else {
      if (Objects.equal(VERSION_QUALIFIER_SNAPSHOT, v2.toUpperCase())) {
        return true;
      } else {
        return v1.compareTo(v2) > 0;
      }
    }
  }
}
