package com.itemis.maven.plugins.unleash.scm;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class MavenScmUtilTest {
  @DataProvider
  public static Object[][] getScmProviderName() {
    return new Object[][] { { "scm:svn:svn://test/svn", "scm:svn:https://test/svn", "svn" },
        { "scm:git:git@test/xyz", null, "git" }, { "scm:svn:svn://test/svn", "", "svn" },
        { "scm:svn:svn://test/svn", " ", "svn" }, { "scm:git:git@test/xyz", "scm:svn:https://test/svn", "git" },
        { null, "scm:git:https://test/xyz", "git" }, { "", "scm:svn:https://test/svn", "svn" },
        { " ", "scm:svn:https://test/svn", "svn" }, { null, null, null }, { null, "", null }, { null, " ", null },
        { "", null, null }, { " ", null, null }, { "scm:git|git@test/xyz", "scm:svn:https://test/svn", "git" },
        { "", "scm:svn|https://test/svn", "svn" } };
  }

  @Test
  @UseDataProvider("getScmProviderName")
  public void testGetScmProviderName(String devConnection, String connection, String expectedProviderName) {
    MavenProject p = new MavenProject();
    p.setScm(new Scm());
    p.getScm().setDeveloperConnection(devConnection);
    p.getScm().setConnection(connection);

    Optional<String> providerName = MavenScmUtil.calcProviderName(p);
    if (expectedProviderName == null) {
      Assert.assertFalse(providerName.isPresent());
    } else {
      Assert.assertTrue(providerName.isPresent());
      Assert.assertEquals(expectedProviderName, providerName.get());
    }
  }
}
