package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Plugin;

import com.google.common.base.Function;
import com.itemis.maven.aether.ArtifactCoordinates;

/**
 * A function to convert a {@link Plugin} to an {@link ArtifactCoordinates} object.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.4.3
 */
public enum PluginToCoordinates implements Function<Plugin, ArtifactCoordinates> {
  INSTANCE;

  // TODO implement tests!
  @Override
  public ArtifactCoordinates apply(Plugin p) {
    return new ArtifactCoordinates(p.getGroupId(), p.getArtifactId(), p.getVersion());
  }
}
