package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class DependencyToStringTest {
  @DataProvider
  public static Object[][] dependencies() {
    return new Object[][] { { createDependency("x", "y", "2.0", null, null), "x:y:2.0" },
        { createDependency("x", "y", null, null, null), "x:y" },
        { createDependency("x", "y", "2.0", "war", null), "x:y:war:2.0" },
        { createDependency("x", "y", "2.0", null, "tests"), "x:y:tests:2.0" },
        { createDependency("x", "y", "2.0", "zip", "model"), "x:y:zip:model:2.0" },
        { createDependency("x", "y", null, "zip", "model"), "x:y:zip:model" } };
  }
  
  @DataProvider
  public static Object[][] dependencies_noType() {
    return new Object[][] { { createDependency("x", "y", "2.0", null, null), "x:y:2.0" },
        { createDependency("x", "y", null, null, null), "x:y" },
        { createDependency("x", "y", "2.0", "war", null), "x:y:2.0" },
        { createDependency("x", "y", "2.0", null, "tests"), "x:y:tests:2.0" },
        { createDependency("x", "y", "2.0", "zip", "model"), "x:y:model:2.0" },
        { createDependency("x", "y", null, "zip", "model"), "x:y:model" } };
  }

  @Test
  @UseDataProvider("dependencies")
  public void testApply(Dependency d, String expected) {
    Assert.assertEquals(expected, DependencyToString.INSTANCE.apply(d));
  }
  
  @Test
  @UseDataProvider("dependencies_noType")
  public void testApply_noType(Dependency d, String expected) {
    Assert.assertEquals(expected, DependencyToString.NO_TYPE.apply(d));
  }

  private static Dependency createDependency(String gid, String aid, String version, String type, String classifier) {
    Dependency d = new Dependency();
    d.setGroupId(gid);
    d.setArtifactId(aid);
    d.setVersion(version);
    d.setType(type);
    d.setClassifier(classifier);
    return d;
  }
}
