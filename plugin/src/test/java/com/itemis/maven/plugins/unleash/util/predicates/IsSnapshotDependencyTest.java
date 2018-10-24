package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;

@RunWith(Parameterized.class)
public class IsSnapshotDependencyTest {

  private static PomPropertyResolver propertyResolver;

  private static IsSnapshotDependency predicate;

  private final Dependency p;

  private final boolean expected;

  public IsSnapshotDependencyTest(Dependency p, boolean expected) {
    this.p = p;
    this.expected = expected;
  }

  @Parameterized.Parameters
  public static Object[][] dependencies() {
    propertyResolver = Mockito.mock(PomPropertyResolver.class);
    predicate = new IsSnapshotDependency(propertyResolver);
    return new Object[][] { { createDependencyAndInitPropertyResolver("1-SNAPSHOT", "1-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver("1", "1"), false },
        { createDependencyAndInitPropertyResolver("1-snapshot", "1-snapshot"), true },
        { createDependencyAndInitPropertyResolver("1-Alpha-SNAPSHOT", "1-Alpha-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver("1.alpha", "1.alpha"), false },
        { createDependencyAndInitPropertyResolver("${version.test1}", "${version.test1}"), false },
        { createDependencyAndInitPropertyResolver("${version.test2}", "1-SNAPSHOT"), true },
        { createDependencyAndInitPropertyResolver(null, null), false } };
  }

  private static Dependency createDependencyAndInitPropertyResolver(String version, String resolverOutput) {
    Mockito.when(propertyResolver.expandPropertyReferences(version)).thenReturn(resolverOutput);

    Dependency d = new Dependency();
    d.setVersion(version);
    return d;
  }

  @Test
  public void TestApply() {
    Assert.assertEquals(expected, predicate.apply(p));
  }
}
