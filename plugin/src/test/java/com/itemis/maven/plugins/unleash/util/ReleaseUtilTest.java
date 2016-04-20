package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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
}
