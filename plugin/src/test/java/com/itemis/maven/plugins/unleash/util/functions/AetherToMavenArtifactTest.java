package com.itemis.maven.plugins.unleash.util.functions;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.Test;

public class AetherToMavenArtifactTest {

  @Test
  public void testApply() {
    org.eclipse.aether.artifact.Artifact a = new DefaultArtifact("x", "y", "tests", "jar", "1.0");
    org.apache.maven.artifact.Artifact result = AetherToMavenArtifact.INSTANCE.apply(a);
    Assert.assertNotNull(result);
    Assert.assertEquals("x", result.getGroupId());
    Assert.assertEquals("y", result.getArtifactId());
    Assert.assertEquals("tests", result.getClassifier());
    Assert.assertEquals("jar", result.getType());
    Assert.assertEquals("1.0", result.getVersion());
  }
}
