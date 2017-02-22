package com.itemis.maven.plugins.unleash.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
// TODO implement tests for upgrade strategy usage!
public class MavenVersionUtilTest {
  @DataProvider
  public static Object[][] calculateReleaseVersion() {
    return new Object[][] { { "3.8.1", "3.8.1" }, { "1.0.0" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT, "1.0.0" },
        { "3.2" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT.toLowerCase(), "3.2" }, { "1.12", "1.12" },
        { "1.3-SNAPSH", "1.3-SNAPSH" }, { "3", "3" }, { "5" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT, "5" } };
  }

  @DataProvider
  public static Object[][] calculateNextSnapshotVersion() {
    return new Object[][] { { "3.8.1", "3.8.2-SNAPSHOT" }, { "1.0.0-SNAPSHOT", "1.0.1-SNAPSHOT" },
        { "1.12", "1.13-SNAPSHOT" }, { "1.3-SNAPSH", "1.4-SNAPSH-SNAPSHOT" }, { "3-Alpha1", "3-Alpha2-SNAPSHOT" },
        { "3-Alpha1-SNAPSHOT", "3-Alpha2-SNAPSHOT" }, { "1-SNAPSHOT", "2-SNAPSHOT" }, { "3", "4-SNAPSHOT" } };
  }

  @DataProvider
  public static Object[][] isSnapshot() {
    return new Object[][] { { "1.0.0" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT, true },
        { "1.0.0" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT.toLowerCase(), true }, { "1.0.0", false },
        { "3.1-Alpha", false }, { "1-SNAPSHOT-alpha", false },
        { "1-alpha" + MavenVersionUtil.VERSION_QUALIFIER_SNAPSHOT, true }, { MavenVersionUtil.VERSION_LATEST, true },
        { MavenVersionUtil.VERSION_LATEST.toLowerCase(), true } };
  }

  @DataProvider
  public static Object[][] isNewerVersion() {
    return new Object[][] { { "1.0.0", "1.0.1", false }, { "1.0.0", "1.0.0", false }, { "1.0.1", "1.0.0", true },
        { "1-SNAPSHOT", "1.1-SNAPSHOT", false }, { "1.1-SNAPSHOT", "1.0-SNAPSHOT", true },
        { "1.0.1-SNAPSHOT", "1.0-SNAPSHOT", true }, { "3.Alpha1", "3.Alpha2", false }, { "3.Alpha2", "3.Alpha1", true },
        { "1-Alpha", "1-Final", false }, { "1.2.Final", "1.2.Alpha", true },
        { MavenVersionUtil.VERSION_LATEST, "1.2", true }, { "3.17.9-SNAPSHOT", MavenVersionUtil.VERSION_LATEST, false },
        { "3", null, true }, { "1.0-SNAPSHOT", "", true }, { "", "1", false }, { null, "1-SNAPSHOT", false } };
  }

  @Test
  @UseDataProvider("calculateReleaseVersion")
  public void testCalculateReleaseVersion(String version, String expectedReleaseVersion) {
    Assert.assertEquals(expectedReleaseVersion, MavenVersionUtil.calculateReleaseVersion(version));
  }

  @Test
  @UseDataProvider("calculateNextSnapshotVersion")
  public void testCalculateNextSnapshotVersion(String version, String expectedSnapshotVersion) {
    Assert.assertEquals(expectedSnapshotVersion, MavenVersionUtil.calculateNextSnapshotVersion(version));
  }

  @Test
  @UseDataProvider("isSnapshot")
  public void testIsSnapshot(String version, boolean expectedResult) {
    Assert.assertEquals(expectedResult, MavenVersionUtil.isSnapshot(version));
  }

  @Test
  @UseDataProvider("isNewerVersion")
  public void testIsNewerVersion(String v1, String v2, boolean expectedResult) {
    Assert.assertEquals(expectedResult, MavenVersionUtil.isNewerVersion(v1, v2));
  }
}
