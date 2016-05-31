package com.itemis.maven.plugins.unleash.scm;

public enum ScmOperation {
  /**
   * Retrieving information about the local and/or remote repository. This includes also querying for the existence of
   * artifacts such as branches or tags.
   */
  INFO,
  /**
   * The creation of a repository branch, either local or remote.
   */
  BRANCH,
  /**
   * Checkout of a remote repository into a local working directory.
   */
  CHECKOUT,
  /**
   * Committing local changes of the working copy to the repository, either local or remote.
   */
  COMMIT,
  /**
   * Deletion of a repository branch, either local or remote.
   */
  DELETE_BRANCH,
  /**
   * Deletion of a repository tag, either local or remote.
   */
  DELETE_TAG,
  /**
   * Merging local and remote changes. For distributed SCMs this means also merging of a fetched upstream branch into
   * the local working copy.
   */
  MERGE,
  /**
   * The push of local commits to the remote repository.
   */
  PUSH,
  /**
   * Retrieval of the repository status (changed, added, removed files and folders).
   */
  STATUS,
  /**
   * The creation of a repository tag, either local or remote.
   */
  TAG,
  /**
   * Update of the local working copy to the state of the remote repository.
   */
  UPDATE,
  /**
   * All other SCM operations.
   */
  UNKNOWN;
}
