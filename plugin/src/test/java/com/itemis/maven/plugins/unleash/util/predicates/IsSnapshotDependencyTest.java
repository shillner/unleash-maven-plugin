package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class IsSnapshotDependencyTest {
  @DataProvider
  public static Object[][] dependencies() {
    return new Object[][] { { createDependency("1-SNAPSHOT"), true }, { createDependency("1"), false },
        { createDependency("1-snapshot"), true }, { createDependency("1-Alpha-SNAPSHOT"), true },
        { createDependency("1.alpha"), false }, { createDependency(null), false } };
  }

  @Test
  @UseDataProvider("dependencies")
  public void TestApply(Dependency d, boolean expected) {
    Assert.assertEquals(expected, IsSnapshotDependency.INSTANCE.apply(d));
  }

  private static Dependency createDependency(String version) {
    Dependency d = new Dependency();
    d.setVersion(version);
    return d;
  }
}
