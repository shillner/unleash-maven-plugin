package com.itemis.maven.plugins.unleash.steps.actions.tycho;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleasePhase;

/**
 * Uses Eclipse Tycho features to upgrade the POMs and MANIFESTs with the release versions. It also updates
 * versions of bundle references.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.1.0
 */
@ProcessingStep(id = "setReleaseVersionsTycho", description = "Uses the tycho-versions-plugin to update the POM and MANIFEST versions for the release.", requiresOnline = true)
public class SetReleaseVersionsTycho extends AbstractTychoVersionsStep implements CDIMojoProcessingStep {
  @Inject
  private Logger log;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Updating project modules with release versions (POM and MANIFEST versions)");
    super.execute(context);
  }

  @Override
  protected ReleasePhase currentReleasePhase() {
    return ReleasePhase.RELEASE;
  }

  @Override
  @RollbackOnError
  public void rollback() throws MojoExecutionException, MojoFailureException {
    this.log.info("Rollback of all version changes necessary for the release (POMs, MANIFESTs, ...).");
    super.rollback();
  }
}
