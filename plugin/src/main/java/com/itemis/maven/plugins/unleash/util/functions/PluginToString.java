package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Plugin;

import com.google.common.base.Function;

/**
 * A function to convert a {@link Plugin} to its String representation in coordinates format.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public enum PluginToString implements Function<Plugin, String> {
  INSTANCE;

  @Override
  public String apply(Plugin p) {
    StringBuilder sb = new StringBuilder(p.getGroupId()).append(":").append(p.getArtifactId());
    if (p.getVersion() != null) {
      sb.append(":").append(p.getVersion());
    }
    return sb.toString();
  }
}
