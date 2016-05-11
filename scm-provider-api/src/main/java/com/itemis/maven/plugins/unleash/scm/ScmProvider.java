package com.itemis.maven.plugins.unleash.scm;

import java.io.File;

import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest;
import com.itemis.maven.plugins.unleash.scm.requests.TagRequest;

public interface ScmProvider {
  void initialize(File workingDirectory);

  void close();

  String commit(CommitRequest request) throws ScmException;

  void push() throws ScmException;

  void update() throws ScmException;

  void tag(TagRequest request) throws ScmException;

  boolean hasTag(String tagName);

  void deleteTag(String tagName) throws ScmException;

  String getLocalRevision();

  String getLatestRemoteRevision();

  String calculateTagConnectionString(String currentConnectionString, String tagName);

  String calculateBranchConnectionString(String currentConnectionString, String branchName);

  boolean isTagInfoIncludedInConnection();
}
