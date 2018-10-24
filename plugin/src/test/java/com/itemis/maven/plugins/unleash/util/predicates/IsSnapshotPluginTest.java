package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Plugin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;

@RunWith(Parameterized.class)
public class IsSnapshotPluginTest {

  private static PomPropertyResolver propertyResolver;

  private static IsSnapshotPlugin predicate;

  private final Plugin p;

  private final boolean expected;

  public IsSnapshotPluginTest(Plugin p, boolean expected) {
    this.p = p;
    this.expected = expected;
  }

  @Parameterized.Parameters
  public static Object[][] plugins() {
    propertyResolver = Mockito.mock(PomPropertyResolver.class);
    predicate = new IsSnapshotPlugin(propertyResolver);
    return new Object[][] { { createPluginAndInitPropertyResolver("1-SNAPSHOT", "1-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver("1", "1"), false },
        { createPluginAndInitPropertyResolver("1-snapshot", "1-snapshot"), true },
        { createPluginAndInitPropertyResolver("1-Alpha-SNAPSHOT", "1-Alpha-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver("1.alpha", "1.alpha"), false },
        { createPluginAndInitPropertyResolver("${version.test1}", "${version.test1}"), false },
        { createPluginAndInitPropertyResolver("${version.test2}", "1-SNAPSHOT"), true },
        { createPluginAndInitPropertyResolver(null, null), false } };
  }

  private static Plugin createPluginAndInitPropertyResolver(String version, String resolverOutput) {
    Mockito.when(propertyResolver.expandPropertyReferences(version)).thenReturn(resolverOutput);

    Plugin p = new Plugin();
    p.setVersion(version);
    return p;
  }

  @Test
  public void TestApply() {
    Assert.assertEquals(expected, predicate.apply(p));
  }
}
