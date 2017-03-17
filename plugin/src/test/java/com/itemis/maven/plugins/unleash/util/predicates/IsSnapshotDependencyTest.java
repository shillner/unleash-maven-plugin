package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class IsSnapshotDependencyTest {
  @DataProvider
  public static Object[][] dependencies() {
    return new Object[][] { { createDependencyAndInitPropertyResolver("1-SNAPSHOT", "1-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver("1", "1"), false },
        { createDependencyAndInitPropertyResolver("1-snapshot", "1-snapshot"), true },
        { createDependencyAndInitPropertyResolver("1-Alpha-SNAPSHOT", "1-Alpha-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver("1.alpha", "1.alpha"), false },
        { createDependencyAndInitPropertyResolver("${version.test1}", "${version.test1}"), false },
        { createDependencyAndInitPropertyResolver("${version.test2}", "1-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver(null, null), false } };
  }

  private static PomPropertyResolver propertyResolver;
  private static IsSnapshotDependency predicate;

  @BeforeClass
  public static void init() {
    propertyResolver = Mockito.mock(PomPropertyResolver.class);
    predicate = new IsSnapshotDependency(propertyResolver);
  }

  @Test
  @UseDataProvider("dependencies")
  public void TestApply(Dependency p, boolean expected) {
    Assert.assertEquals(expected, predicate.apply(p));
  }

  private static Dependency createDependencyAndInitPropertyResolver(String version, String resolverOutput) {
    Mockito.when(propertyResolver.expandPropertyReferences(version)).thenReturn(resolverOutput);

    Dependency d = new Dependency();
    d.setVersion(version);
    return d;
  }
}
