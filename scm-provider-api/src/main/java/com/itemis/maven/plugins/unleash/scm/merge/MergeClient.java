package com.itemis.maven.plugins.unleash.scm.merge;

import java.io.File;
import java.io.OutputStream;

import com.itemis.maven.plugins.unleash.scm.ScmException;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;

/**
 * The callback interface used by the {@link ScmProvider ScmProviders} for merge conflict resolution.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public interface MergeClient {

  /**
   * Merges conflicting local and remote changes and writes back the merge result into the output stream.
   * 
   * @param local the local copy of the conflicting file.
   * @param remote the remote copy of the conflicting file.
   * @param base the base copy of the conflicting file which is the ancestor of both, the local and remote file.
   * @param result an output stream to which the merge result can be written. This output stream will be closed
   *          automatically afterwards.
   * @throws ScmException if anything goes wrong during the merge operation.
   */
  void merge(File local, File remote, File base, OutputStream result) throws ScmException;
}
