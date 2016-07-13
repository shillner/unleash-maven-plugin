package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

import com.google.common.base.Function;

/**
 * A function to convert an {@link org.eclipse.aether.artifact.Artifact} to an
 * {@link org.apache.maven.artifact.Artifact}.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public enum AetherToMavenArtifact
    implements Function<org.eclipse.aether.artifact.Artifact, org.apache.maven.artifact.Artifact> {
  INSTANCE;

  @Override
  public org.apache.maven.artifact.Artifact apply(org.eclipse.aether.artifact.Artifact a) {
    return new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), "compile", a.getExtension(),
        a.getClassifier() != null ? a.getClassifier() : null, new DefaultArtifactHandler());
  }
}
