package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;

/**
 * A predicate determining whether a {@link Project} has a SNAPSHOT version assigned.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public enum IsSnapshotProject implements Predicate<MavenProject> {
  INSTANCE;

  @Override
  public boolean apply(MavenProject p) {
    String version = p.getVersion();
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
