package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class IsSnapshotProjectTest {
  @DataProvider
  public static Object[][] projects() {
    return new Object[][] { { createProject("1-SNAPSHOT"), true }, { createProject("1"), false },
        { createProject("1-snapshot"), true }, { createProject("1-Alpha-SNAPSHOT"), true },
        { createProject("1.alpha"), false } };
  }

  @Test
  @UseDataProvider("projects")
  public void TestApply(MavenProject p, boolean expected) {
    Assert.assertEquals(expected, IsSnapshotProject.INSTANCE.apply(p));
  }

  private static MavenProject createProject(String version) {
    MavenProject p = new MavenProject();
    p.setGroupId("x");
    p.setArtifactId("x");
    p.setVersion(version);
    return p;
  }
}
