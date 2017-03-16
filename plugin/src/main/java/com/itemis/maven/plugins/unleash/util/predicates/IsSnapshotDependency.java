package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Dependency;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;

/**
 * A predicate determining whether a {@link Dependency} has a SNAPSHOT version assigned.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public class IsSnapshotDependency implements Predicate<Dependency> {
  private PomPropertyResolver propertyResolver;

  public IsSnapshotDependency(PomPropertyResolver propertyResolver) {
    this.propertyResolver = propertyResolver;
  }

  @Override
  public boolean apply(Dependency d) {
    String version = d.getVersion();
    version = propertyResolver.expandPropertyReferences(version);
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
