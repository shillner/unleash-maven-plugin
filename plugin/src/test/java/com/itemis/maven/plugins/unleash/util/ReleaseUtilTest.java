package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ReleaseUtilTest {
  @DataProvider
  public static Object[][] getTagName() {
    MavenProject p = new MavenProject();
    p.setGroupId("P_GID");
    p.setArtifactId("P_AID");
    p.setVersion("P_VERSION");
    return new Object[][] { { "test", p, "test" }, { "@{project.version}", p, "P_VERSION" },
        { "@{project.groupId}", p, "P_GID" }, { "@{project.artifactId}", p, "P_AID" },
        { "xyz-@{project.version}-@{project.artifactId}", p, "xyz-P_VERSION-P_AID" }, { "@{xyz}", p, "" } };
  }

  @DataProvider
  public static Object[][] getTagName_Exception() {
    MavenProject p = new MavenProject();
    p.setGroupId("P_GID");
    p.setArtifactId("P_AID");
    p.setVersion("P_VERSION");
    return new Object[][] { { "test", null }, { null, p }, { null, null } };
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  @UseDataProvider("getTagName")
  public void testGetTagName(String tagNamePattern, MavenProject project, String expectedTagName) {
    String name = ReleaseUtil.getTagName(tagNamePattern, project);
    Assert.assertEquals(expectedTagName, name);
  }

  @Test
  @UseDataProvider("getTagName_Exception")
  public void testGetTagNameNoProject(String tagNamePattern, MavenProject project) {
    this.exception.expect(IllegalArgumentException.class);
    ReleaseUtil.getTagName(tagNamePattern, project);
  }

  @Test
  @DataProvider({ "1-SNAPSHOT,null,1", "1.0.0-SNAPSHOT,null,1.0.0", "3.1,null,3.1", "3.Alpha1-SNAPSHOT,null,3.Alpha1",
      "2.4,3,3", "2.1-SNAPSHOT,3,3" })
  public void testGetReleaseVersion(String version, String defaultReleaseVersion, String expected) {
    Assert.assertEquals(expected, ReleaseUtil.getReleaseVersion(version, Optional.fromNullable(defaultReleaseVersion),
        Optional.<Prompter> absent()));
  }

  @Test
  @DataProvider({ "1-SNAPSHOT,null,4,4", "2-SNAPSHOT,8,4,8" })
  public void testGetReleaseVersion_Prompter(String version, String defaultReleaseVersion, String userInput,
      String expected) throws Exception {
    Prompter prompter = Mockito.mock(Prompter.class);
    Mockito.when(prompter.prompt((String) Matchers.notNull(), (String) Matchers.notNull())).thenReturn(userInput);
    Assert.assertEquals(expected,
        ReleaseUtil.getReleaseVersion(version, Optional.fromNullable(defaultReleaseVersion), Optional.of(prompter)));
  }

  @Test
  @DataProvider({ "1-SNAPSHOT,null,2-SNAPSHOT", "1.0.0-SNAPSHOT,null,1.0.1-SNAPSHOT", "3.1,null,3.2-SNAPSHOT",
      "3.Alpha1-SNAPSHOT,null,3.Alpha2-SNAPSHOT", "2.4,3-SNAPSHOT,3-SNAPSHOT",
      "2.1.Alpha-SNAPSHOT,null,2.2.Alpha-SNAPSHOT" })
  public void testGetNextDevelopmentVersion(String version, String defaultDevVersion, String expected) {
    Assert.assertEquals(expected, ReleaseUtil.getNextDevelopmentVersion(version,
        Optional.fromNullable(defaultDevVersion), Optional.<Prompter> absent()));
  }

  @Test
  @DataProvider({ "1-SNAPSHOT,null,4-SNAPSHOT,4-SNAPSHOT", "2-SNAPSHOT,8-SNAPSHOT,4-SNAPSHOT,8-SNAPSHOT" })
  public void testGetNextDevelopmentVersion_Prompter(String version, String defaultReleaseVersion, String userInput,
      String expected) throws Exception {
    Prompter prompter = Mockito.mock(Prompter.class);
    Mockito.when(prompter.prompt((String) Matchers.notNull(), (String) Matchers.notNull())).thenReturn(userInput);
    Assert.assertEquals(expected, ReleaseUtil.getNextDevelopmentVersion(version,
        Optional.fromNullable(defaultReleaseVersion), Optional.of(prompter)));
  }
}
