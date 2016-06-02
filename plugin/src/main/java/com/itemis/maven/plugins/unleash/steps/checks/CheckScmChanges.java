package com.itemis.maven.plugins.unleash.steps.checks;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Objects;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

@ProcessingStep(id = "checkForScmChanges", description = "Checks the SCM for changes that would require stopping the release.", requiresOnline = true)
public class CheckScmChanges implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("commitBeforeTagging")
  private boolean commitBeforeTagging;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!this.commitBeforeTagging) {
      this.log.debug("No commit before tagging requested. Checking for SCM changes at this point unnessecary!");
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
