package com.itemis.maven.plugins.unleash.steps.checks;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.ScmProviderRegistry;

@ProcessingStep(@Goal(name = "perform", stepNumber = 60))
public class CheckScmChanges implements CDIMojoProcessingStep {

  @Inject
  private ScmProviderRegistry scmProviderRegistry;

  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Optional<ScmProvider> provider = this.scmProviderRegistry.getProvider();
    if (!provider.isPresent()) {
      throw new MojoFailureException(
          "Could not load the SCM provider, please check previous log entries. Maybe you need to add an appropriate provider implementation as a dependency to the plugin.");
    }

    String latestRemoteRevision = provider.get().getLatestRemoteRevision();
    this.metadata.setScmRevision(latestRemoteRevision, ReleasePhase.POST);

    if (!Objects.equal(latestRemoteRevision, this.metadata.getScmRevision(ReleasePhase.PRE))) {
      throw new MojoFailureException(
          "The local working copy which has been built is out of sync with the remote repository. [Local revision: "
              + this.metadata.getScmRevision(ReleasePhase.PRE) + "] [Latest remote revision: " + latestRemoteRevision
              + "]");
    }
  }
}
