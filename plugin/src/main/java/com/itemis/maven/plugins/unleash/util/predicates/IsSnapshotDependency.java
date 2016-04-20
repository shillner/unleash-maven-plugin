package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Dependency;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;

public enum IsSnapshotDependency implements Predicate<Dependency> {
  INSTANCE;

  @Override
  public boolean apply(Dependency d) {
    String version = d.getVersion();
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
