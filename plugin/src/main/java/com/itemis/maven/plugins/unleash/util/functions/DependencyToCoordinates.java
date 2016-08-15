package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Dependency;

import com.google.common.base.Function;
import com.itemis.maven.aether.ArtifactCoordinates;

/**
 * A function to convert a {@link Dependency} to appropriate {@link ArtifactCoordinates}.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.3.0
 */
public enum DependencyToCoordinates implements Function<Dependency, ArtifactCoordinates> {
  INSTANCE(true), NO_CLASSIFIER(false);

  private boolean includeClassifier;

  private DependencyToCoordinates(boolean includeClassifier) {
    this.includeClassifier = includeClassifier;
  }

  @Override
  public ArtifactCoordinates apply(Dependency d) {
    if (this.includeClassifier) {
      return new ArtifactCoordinates(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), d.getClassifier());
    }
    return new ArtifactCoordinates(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType());
  }
}
