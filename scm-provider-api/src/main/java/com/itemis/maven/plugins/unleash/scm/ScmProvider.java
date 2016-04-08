package com.itemis.maven.plugins.unleash.scm;

public interface ScmProvider {
  String getLocalRevision();

  String getLatestRemoteRevision();
}
