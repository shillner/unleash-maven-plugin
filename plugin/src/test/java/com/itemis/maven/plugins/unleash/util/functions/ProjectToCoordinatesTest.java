package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ProjectToCoordinatesTest {
  @DataProvider
  public static Object[][] projects_full() {
    return new Object[][] { { createProject("x", "y", "2.0", null), new ArtifactCoordinates("x", "y", "2.0", null) },
        { createProject("x", "y", "13", "war"), new ArtifactCoordinates("x", "y", "13", "war") } };
  }

  @DataProvider
  public static Object[][] projects_emptyVersion() {
    return new Object[][] {
        { createProject("x", "y", "3", null),
            new ArtifactCoordinates("x", "y", MavenProject.EMPTY_PROJECT_VERSION, null) },
        { createProject("x", "y", "13", "war"),
            new ArtifactCoordinates("x", "y", MavenProject.EMPTY_PROJECT_VERSION, "war") } };
  }

  @DataProvider
  public static Object[][] projects_pomPackaging() {
    return new Object[][] {
        { createProject("x", "y", "3", null), new ArtifactCoordinates("x", "y", "3", PomUtil.ARTIFACT_TYPE_POM) },
        { createProject("x", "y", "13", "war"), new ArtifactCoordinates("x", "y", "13", PomUtil.ARTIFACT_TYPE_POM) } };
  }

  @DataProvider
  public static Object[][] projects_emptyVersion_pomPackaging() {
    return new Object[][] {
        { createProject("x", "y", "3", null),
            new ArtifactCoordinates("x", "y", MavenProject.EMPTY_PROJECT_VERSION, PomUtil.ARTIFACT_TYPE_POM) },
        { createProject("x", "y", "13", "war"),
            new ArtifactCoordinates("x", "y", MavenProject.EMPTY_PROJECT_VERSION, PomUtil.ARTIFACT_TYPE_POM) } };
  }

  @Test
  @UseDataProvider("projects_full")
  public void testApply(MavenProject p, ArtifactCoordinates expected) {
    Assert.assertEquals(expected, ProjectToCoordinates.INSTANCE.apply(p));
  }

  @Test
  @UseDataProvider("projects_emptyVersion")
  public void testApply_emptyVersion(MavenProject p, ArtifactCoordinates expected) {
    Assert.assertEquals(expected, ProjectToCoordinates.EMPTY_VERSION.apply(p));
  }

  @Test
  @UseDataProvider("projects_pomPackaging")
  public void testApply_pomPackaging(MavenProject p, ArtifactCoordinates expected) {
    Assert.assertEquals(expected, ProjectToCoordinates.POM.apply(p));
  }

  @Test
  @UseDataProvider("projects_emptyVersion_pomPackaging")
  public void testApply_emptyVersion_pomPackaging(MavenProject p, ArtifactCoordinates expected) {
    Assert.assertEquals(expected, ProjectToCoordinates.EMPTY_VERSION_POM.apply(p));
  }

  private static MavenProject createProject(String gid, String aid, String version, String packaging) {
    MavenProject p = new MavenProject();
    p.setGroupId(gid);
    p.setArtifactId(aid);
    p.setVersion(version);
    p.setPackaging(packaging);
    return p;
  }
}
