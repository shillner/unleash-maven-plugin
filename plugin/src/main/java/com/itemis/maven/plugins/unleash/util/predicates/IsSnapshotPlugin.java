package com.itemis.maven.plugins.unleash.util.predicates;

import org.apache.maven.model.Plugin;

import com.google.common.base.Predicate;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;

public enum IsSnapshotPlugin implements Predicate<Plugin> {
  INSTANCE;

  @Override
  public boolean apply(Plugin p) {
    String version = p.getVersion();
    return version != null && MavenVersionUtil.isSnapshot(version);
  }
}
