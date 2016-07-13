package com.itemis.maven.plugins.unleash.steps.checks;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Objects;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

/**
 * Checks the remote SCM repository for changes in the case that a commit was requested before tagging the repository.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "checkForScmChanges", description = "Checks the remote SCM repository for changes that would require stopping the release. This will only be executed if a commit was requested before tagging the repository.", requiresOnline = true)
public class CheckScmChanges implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("commitBeforeTagging")
  private boolean commitBeforeTagging;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info(
        "Checking remote SCM repository for changes. Initial revision was " + this.metadata.getInitialScmRevision());
    if (!this.commitBeforeTagging) {
      this.log.debug("\tNo commit before tagging requested. Checking for SCM changes at this point unnessecary!");
      return;
    }

    ScmProvider provider = this.scmProviderRegistry.getProvider();
    String latestRemoteRevision = provider.getLatestRemoteRevision();
    if (!Objects.equal(latestRemoteRevision, this.metadata.getInitialScmRevision())) {
      throw new MojoFailureException(
          "The local working copy which has been built is out of sync with the remote repository. [Local revision: "
              + this.metadata.getInitialScmRevision() + "] [Latest remote revision: " + latestRemoteRevision + "]");
    }
  }
}
