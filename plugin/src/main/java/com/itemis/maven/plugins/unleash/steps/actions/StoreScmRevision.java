package com.itemis.maven.plugins.unleash.steps.actions;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.ScmProviderRegistry;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

/**
 * A Mojo that just stores the local SCM revision information in the release metadata. This information is needed in a
 * later step to ensure that no other commits where done while releasing the artifact.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
// QUESTION: can we omit this step? revision is fetched from local working directory only so we could also just compare
// local and remote revisions when it is time to commit!
@ProcessingStep(@Goal(name = "perform", stepNumber = 1))
public class StoreScmRevision implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;
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

    String revision = provider.get().getLocalRevision();
    this.metadata.setScmRevision(revision, ReleasePhase.PRE_RELEASE);
    this.log.info("SCM Revision before releasing the artifacts: " + revision);
  }
}
