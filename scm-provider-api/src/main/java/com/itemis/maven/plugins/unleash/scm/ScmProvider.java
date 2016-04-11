package com.itemis.maven.plugins.unleash.scm;

import java.io.File;

public interface ScmProvider {
  void initialize(File workingDirectory);

  void close();

  String getLocalRevision();

  String getLatestRemoteRevision();
}
