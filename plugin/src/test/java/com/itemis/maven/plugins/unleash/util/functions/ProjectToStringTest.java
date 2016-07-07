package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ProjectToStringTest {
  @DataProvider
  public static Object[][] projects_includePackaging() {
    return new Object[][] { { createProject("x", "y", "2.0", null), "x:y:2.0" },
        { createProject("x", "y", "13", "war"), "x:y:war:13" } };
  }

  @DataProvider
  public static Object[][] projects_excludePackaging() {
    return new Object[][] { { createProject("x", "y", "2.0", null), "x:y:2.0" },
        { createProject("x", "y", "13", "war"), "x:y:13" } };
  }

  @Test
  @UseDataProvider("projects_includePackaging")
  public void testApply_IncludePackaging(MavenProject p, String expected) {
    Assert.assertEquals(expected, ProjectToString.INCLUDE_PACKAGING.apply(p));
  }

  @Test
  @UseDataProvider("projects_excludePackaging")
  public void testApply_ExcludePackaging(MavenProject p, String expected) {
    Assert.assertEquals(expected, ProjectToString.INSTANCE.apply(p));
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
