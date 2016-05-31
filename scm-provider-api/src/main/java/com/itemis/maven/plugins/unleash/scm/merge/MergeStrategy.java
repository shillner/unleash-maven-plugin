package com.itemis.maven.plugins.unleash.scm.merge;

/**
 * Different merge strategies that must be supported by concrete SCM provider implementations during merge conflict
 * resolution.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public enum MergeStrategy {
  /**
   * No merge requested. The concrete outcome is unspecified but must not be a merge of the files.
   */
  DO_NOT_MERGE,
  /**
   * Automatically merge files by using the local changes in case of a conflict.
   */
  USE_LOCAL,
  /**
   * Automatically merge files by using the remote changes in case of a conflict.
   */
  USE_REMOTE,
  /**
   * Automatically merge files as far as possible but use a callback ({@link MergeClient}) for merge conflict
   * resolution.
   */
  FULL_MERGE;
}
