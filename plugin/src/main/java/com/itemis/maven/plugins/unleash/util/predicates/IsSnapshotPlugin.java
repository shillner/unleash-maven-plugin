package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Plugin;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;

/**
 * A predicate determining whether a {@link Plugin} has a SNAPSHOT version assigned.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public class IsSnapshotPlugin implements Predicate<Plugin> {
  private PomPropertyResolver propertyResolver;

  public IsSnapshotPlugin(PomPropertyResolver propertyResolver) {
    this.propertyResolver = propertyResolver;
  }

  @Override
  public boolean apply(Plugin p) {
    String version = p.getVersion();
    version = propertyResolver.expandPropertyReferences(version);
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
