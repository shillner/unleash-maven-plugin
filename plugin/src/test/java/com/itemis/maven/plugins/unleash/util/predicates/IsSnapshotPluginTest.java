package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Plugin;
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
public class IsSnapshotPluginTest {
  @DataProvider
  public static Object[][] plugins() {
    return new Object[][] { { createPluginAndInitPropertyResolver("1-SNAPSHOT", "1-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver("1", "1"), false },
        { createPluginAndInitPropertyResolver("1-snapshot", "1-snapshot"), true },
        { createPluginAndInitPropertyResolver("1-Alpha-SNAPSHOT", "1-Alpha-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver("1.alpha", "1.alpha"), false },
        { createPluginAndInitPropertyResolver("${version.test1}", "${version.test1}"), false },
        { createPluginAndInitPropertyResolver("${version.test2}", "1-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver(null, null), false } };
  }

  private static PomPropertyResolver propertyResolver;
  private static IsSnapshotPlugin predicate;

  @BeforeClass
  public static void init() {
    propertyResolver = Mockito.mock(PomPropertyResolver.class);
    predicate = new IsSnapshotPlugin(propertyResolver);
  }

  @Test
  @UseDataProvider("plugins")
  public void TestApply(Plugin p, boolean expected) {
    Assert.assertEquals(expected, predicate.apply(p));
  }

  private static Plugin createPluginAndInitPropertyResolver(String version, String resolverOutput) {
    Mockito.when(propertyResolver.expandPropertyReferences(version)).thenReturn(resolverOutput);

    Plugin p = new Plugin();
    p.setVersion(version);
    return p;
  }
}
