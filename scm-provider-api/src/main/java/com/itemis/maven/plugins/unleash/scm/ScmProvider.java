package com.itemis.maven.plugins.unleash.scm;

import java.io.File;

public interface ScmProvider {
  void initialize(File workingDirectory);

  void close();

  String getLocalRevision();

  String getLatestRemoteRevision();

  void tag(String tagName, String revision, String message) throws ScmException;

  boolean hasTag(String tagName);

  void deleteTag(String tagName) throws ScmException;

  String calculateTagConnectionString(String currentConnectionString, String tagName);

  String calculateBranchConnectionString(String currentConnectionString, String branchName);

  boolean isTagInfoIncludedInConnection();
}
