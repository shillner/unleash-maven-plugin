package com.itemis.maven.plugins.unleash.util.functions;

import java.io.File;

import com.google.common.base.Function;

/**
 * A function to convert a file's absolute path into the relative path starting from a reference file.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
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
