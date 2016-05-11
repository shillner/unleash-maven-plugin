package com.itemis.maven.plugins.unleash.util;

/**
 * A utility class for maven version management.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public final class MavenVersionUtil {
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
    return version.toUpperCase().endsWith(PomUtil.VERSION_QUALIFIER_SNAPSHOT)
        || version.toUpperCase().equals(PomUtil.VERSION_LATEST);
  }

  /**
   * Calculates the next higher SNAPSHOT version of the passed version String by incrementing the lowest possible number
   * segment of the version String by one.<br>
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
   * @return the next higher SNAPSHOT version.
   */
  public static String calculateNextSnapshotVersion(String version) {
    StringBuilder sb;
    if (version.endsWith(PomUtil.VERSION_QUALIFIER_SNAPSHOT)) {
      sb = new StringBuilder(version.substring(0, version.length() - PomUtil.VERSION_QUALIFIER_SNAPSHOT.length()));
    } else {
      sb = new StringBuilder(version);
    }

    int start = -1;
    int end = -1;
    for (int i = sb.length() - 1; i >= 0; i--) {
      try {
        Integer.parseInt(sb.substring(i, i + 1));
        if (end == -1) {
          end = i;
        }
      } catch (NumberFormatException e) {
        if (end > 0 && start == -1) {
          start = i + 1;
          break;
        }
      }
    }

    if (end >= 0 && start == -1) {
      start = 0;
    }

    int versionSegmentToIncrease = Integer.parseInt(sb.substring(start, end + 1));
    sb.replace(start, end + 1, Integer.toString(versionSegmentToIncrease + 1));
    sb.append(PomUtil.VERSION_QUALIFIER_SNAPSHOT);

    return sb.toString();
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
    if (version.toUpperCase().endsWith(PomUtil.VERSION_QUALIFIER_SNAPSHOT)) {
      return version.substring(0, version.length() - PomUtil.VERSION_QUALIFIER_SNAPSHOT.length());
    }
    return version;
  }
}
