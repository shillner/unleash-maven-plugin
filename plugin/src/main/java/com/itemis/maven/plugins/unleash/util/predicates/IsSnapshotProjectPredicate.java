package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;

public class IsSnapshotProjectPredicate implements Predicate<MavenProject> {

  @Override
  public boolean apply(MavenProject p) {
    String version = p.getVersion();
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
