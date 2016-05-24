package com.itemis.maven.plugins.unleash.util.functions;

import java.io.File;

import com.google.common.base.Function;

public class FileToRelativePath implements Function<File, String> {
  private File workingDir;

  public FileToRelativePath(File workingDir) {
    this.workingDir = workingDir;
  }

  @Override
  public String apply(File f) {
    return this.workingDir.toURI().relativize(f.toURI()).toString();
  }
}
