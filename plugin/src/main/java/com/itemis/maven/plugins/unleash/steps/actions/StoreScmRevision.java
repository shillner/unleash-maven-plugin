package com.itemis.maven.plugins.unleash.steps.actions;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

/**
 * A Mojo that just stores the local SCM revision information in the release metadata. This information is needed in a
 * later step to ensure that no other commits where done while releasing the artifact.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "storeScmRevision", description = "Stores the checked out SCM revision in the release metadata for later usage.", requiresOnline = false)
public class StoreScmRevision implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;
  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    ScmProvider provider = this.scmProviderRegistry.getProvider();
    String revision = provider.getLocalRevision();
    this.metadata.setInitialScmRevision(revision);
    this.log.info("SCM Revision before releasing the artifacts: " + revision);
  }
}
