package com.itemis.maven.plugins.unleash.scm;

import java.io.File;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.unleash.scm.annotations.ScmProviderType;
import com.itemis.maven.plugins.unleash.scm.requests.BranchRequest;
import com.itemis.maven.plugins.unleash.scm.requests.CheckoutRequest;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest;
import com.itemis.maven.plugins.unleash.scm.requests.DeleteBranchRequest;
import com.itemis.maven.plugins.unleash.scm.requests.DeleteTagRequest;
import com.itemis.maven.plugins.unleash.scm.requests.DiffRequest;
import com.itemis.maven.plugins.unleash.scm.requests.HistoryRequest;
import com.itemis.maven.plugins.unleash.scm.requests.PushRequest;
import com.itemis.maven.plugins.unleash.scm.requests.RevertCommitsRequest;
import com.itemis.maven.plugins.unleash.scm.requests.TagRequest;
import com.itemis.maven.plugins.unleash.scm.requests.UpdateRequest;
import com.itemis.maven.plugins.unleash.scm.results.DiffResult;
import com.itemis.maven.plugins.unleash.scm.results.HistoryResult;

/**
 * SCM providers for the unleash-maven-plugin must implement this interface to provide SCM-specific access for the
 * plugin.
 * Furthermore the providers must be annotated witht the {@link ScmProviderType} annotation providing the appropriate
 * SCM type name.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 0.1.0
 */
public interface ScmProvider {
  // TODO also provide an option for keyfiles, ...
  // system or repo keyring?

  /**
   * Initializes the SCM provider with the working directory and some other optional parameters such as credentials.
   *
   * @param workingDirectory the working directory on which the scm provider has to do its work.
   * @param logger the logger to be used with this provider. If not logger is set a standard logger bound to the
   *          interface {@link ScmProvider} is used.
   * @param username the username for remote SCM access.
   * @param password the password for remote SCM access.
   */
  void initialize(File workingDirectory, Optional<Logger> logger, Optional<String> username, Optional<String> password);

  /**
   * Closes the SCM provider and releases all bound resources.
   */
  void close();

  /**
   * Checks out a remote repository into the working directory which has been set during provider initialization.<br>
   * The checkout can be performed from any branch or tag with optional specification of revision information.<br>
   * <br>
   * <b>Note that the working directory of this provider instance must be empty for checkout operations!</b>
   *
   * @param request the description of what to checkout from where.
   * @throws ScmException if f.i. the working dir is not empty or anything else happens during checkout.
   */
  void checkout(CheckoutRequest request) throws ScmException;

  /**
   * Commits the specified paths or the whole working directory changes. For distributed SCMs a push to the remote
   * repository can be specified. If there are remote changes the provider tries to commit the local changes or fails if
   * there are conflicts.
   *
   * @param request the request containing all relevant settings for the commit.
   * @return the new revision number after the commit which is the remote revision or the new local revision for
   *         distributed SCMs when pushing is not enabled.
   * @throws ScmException if either the commit or the push to the remote repo encountered an error.
   */
  String commit(CommitRequest request) throws ScmException;

  /**
   * Pushes the local changes to the remote repository which is relevant for distributed SCMs only.
   *
   * @param request the request containing all relevant settings for the push.
   * @return the new remote revision after the push has been executed successfully.
   * @throws ScmException if the push encountered an error, f.i. if the remote repo is ahead, ...
   */
  String push(PushRequest request) throws ScmException;

  /**
   * Updates the local repository with changes of the remote repository which might fail due to conflicts. Merging can
   * also be required.
   *
   * @param request the request describing what to update to which revision and how to deal with conflicts.
   * @return the new revision after merging remote changes. This might be the requested remote revision which has been
   *         merged or a newer one if a merge commit was necessary.
   * @throws ScmException if the update fails, f.i. due to unresolvable conflicts.
   */
  String update(UpdateRequest request) throws ScmException;

  /**
   * Creates a tag on the local (and optionally the remote) repository either from the working copy or from a specified
   * remote repository location.
   *
   * @param request the request specifying all relevant information for the tag creation.
   * @return the new revision number after the tag has been created which is the remote revision or the new local
   *         revision for distributed SCMs when pushing is not enabled.
   * @throws ScmException if the tag could not be created.
   */
  String tag(TagRequest request) throws ScmException;

  /**
   * @param tagName the name of the tag which is searched.
   * @return <code>true</code> if the repository contains a tag with the specified name.
   * @throws ScmException if the repository access (local or remote) fails and no exact information about the presence
   *           of the tag can be given.
   */
  boolean hasTag(String tagName) throws ScmException;

  /**
   * Deletes a tag from the repository. In case of distributed SCMs the deletion can only happen locally or also on the
   * remote repository if pushing is enabled.
   *
   * @param request the deletion request specifying all relevant information for tag deletion.
   * @return the new revision number after the tag deletion which is the remote revision or the new local revision for
   *         distributed SCMs when pushing is not enabled.
   * @throws ScmException if the tag could not be deleted.
   */
  String deleteTag(DeleteTagRequest request) throws ScmException;

  /**
   * Creates a branch from the local (and optionally the remote) repository either from the working copy or from a
   * specified remote repository location.
   *
   * @param request the request specifying all relevant information for the branch creation.
   * @return the new revision number after the branch has been created which is the remote revision or the new local
   *         revision for distributed SCMs when pushing is not enabled.
   * @throws ScmException if the branch could not be created.
   */
  String branch(BranchRequest request) throws ScmException;

  /**
   * @param branchName the name of the branch which is searched.
   * @return <code>true</code> if the repository contains a branch with the specified name.
   * @throws ScmException if the repository access (local or remote) fails and no exact information about the presence
   *           of the branch can be given.
   */
  boolean hasBranch(String branchName) throws ScmException;

  /**
   * Deletes a branch from the repository. In case of distributed SCMs the deletion can only happen locally or also on
   * the
   * remote repository if pushing is enabled.
   *
   * @param request the deletion request specifying all relevant information for branch deletion.
   * @return the new revision number after the branch deletion which is the remote revision or the new local revision
   *         for
   *         distributed SCMs when pushing is not enabled.
   * @throws ScmException if the branch could not be deleted.
   */
  String deleteBranch(DeleteBranchRequest request) throws ScmException;

  /**
   * Reverts a number of commits in local and remote repository. If the commits to revert are older than the actual HEAD
   * the diff gets merged on top of the HEAD revision.
   *
   * @param request the request containing the revision range to be reverted and merge information.
   * @return the latest remote revision on the current branch.
   * @throws ScmException if f.i. the specified revisions are newer than the HEAD or FROM and TO are specified in the
   *           wrong order or anything happens during the reversion.
   */
  String revertCommits(RevertCommitsRequest request) throws ScmException;

  /**
   * @return the revision of the current working copy.
   */
  String getLocalRevision();

  /**
   * @return the latest remote revision. In case of distributed SCMs the remote revision is retrieved from the remote
   *         the current branch is tracking.
   */
  String getLatestRemoteRevision();

  /**
   * Calculates the URL of a tag with the given tag name from another connection URL which might just be a basic
   * URL or a URL containing branch info, ...
   *
   * @param currentConnectionString a connection URL from which the tag URL shall be derived assuming standard
   *          directory layout.
   * @param tagName the name of the branch.
   * @return the calculated tag URL.
   */
  String calculateTagConnectionString(String currentConnectionString, String tagName);

  /**
   * Calculates the URL of the branch with the given branch name from another connection URL which might just be a basic
   * URL or a URL containing branch info, ...
   *
   * @param currentConnectionString a connection URL from which the branch URL shall be derived assuming standard
   *          directory layout.
   * @param branchName the name of the branch.
   * @return the calculated branch URL.
   */
  String calculateBranchConnectionString(String currentConnectionString, String branchName);

  /**
   * @return <code>true</code> if the SCM connection URL comprises also the tag name (and path).
   */
  boolean isTagInfoIncludedInConnection();

  /**
   * Calculates the history of the repository which can happen locally only remote-only or combined. Through the request
   * a variety of query conditions can be specified for smaller history results and faster queries, such as a commit
   * range or the maximum number of results.
   *
   * @param request the history request for specifying the history conditions.
   * @return the history produced by the query.
   * @throws ScmException if the SCM provider implementation encountered any error querying the repository history.
   */
  HistoryResult getHistory(HistoryRequest request) throws ScmException;

  /**
   * Calculates a diff between the requested repository paths and objects. The result may only contain diff status
   * information or a full textual diff for each changed object.
   *
   * @param request the request which specifies the diff calculation requirements.
   * @return a result containing a diff object for each changed object of the repository.
   * @throws ScmException if the underlying scm provider implementation encountered an error while creating the diff.
   *           The operation type is alway DIFF.
   */
  DiffResult getDiff(DiffRequest request) throws ScmException;
}
