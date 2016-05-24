package com.itemis.maven.plugins.unleash.scm.merge;

import java.io.File;
import java.io.OutputStream;

import com.itemis.maven.plugins.unleash.scm.ScmException;

public interface MergeClient {

  void merge(File local, File remote, File base, OutputStream result) throws ScmException;
}
