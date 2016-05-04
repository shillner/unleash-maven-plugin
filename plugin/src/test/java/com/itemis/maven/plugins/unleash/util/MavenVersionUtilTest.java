package com.itemis.maven.plugins.unleash.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class MavenVersionUtilTest {
  @DataProvider
  public static Object[][] calculateReleaseVersion() {
    return new Object[][] { { "3.8.1", "3.8.1" }, { "1.0.0-SNAPSHOT", "1.0.0" }, { "1.12", "1.12" },
        { "1.3-SNAPSH", "1.3-SNAPSH" }, { "3", "3" }, { "5-SNAPSHOT", "5" } };
  }

  @DataProvider
  public static Object[][] calculateNextSnapshotVersion() {
    return new Object[][] { { "3.8.1", "3.8.2-SNAPSHOT" }, { "1.0.0-SNAPSHOT", "1.0.1-SNAPSHOT" },
        { "1.12", "1.13-SNAPSHOT" }, { "1.3-SNAPSH", "1.4-SNAPSH-SNAPSHOT" }, { "3-Alpha1", "3-Alpha2-SNAPSHOT" },
        { "3-Alpha1-SNAPSHOT", "3-Alpha2-SNAPSHOT" } };
  }

  @DataProvider
  public static Object[][] isSnapshot() {
    return new Object[][] { { "1.0.0" + PomUtil.VERSION_QUALIFIER_SNAPSHOT, true },
        { "1.0.0" + PomUtil.VERSION_QUALIFIER_SNAPSHOT.toLowerCase(), true }, { "1.0.0", false },
        { "3.1-Alpha", false }, { "1-SNAPSHOT-alpha", false }, { "1-alpha" + PomUtil.VERSION_QUALIFIER_SNAPSHOT, true },
        { PomUtil.VERSION_LATEST, true }, { PomUtil.VERSION_LATEST.toLowerCase(), true } };
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
}
