package com.itemis.maven.plugins.unleash.steps.checks;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

@ProcessingStep(id = "checkParentVersion", description = "Checks that the project does not reference a SNAPSHOT parent.", requiresOnline = false)
public class CheckParentVersion implements CDIMojoProcessingStep {
  @Inject
  private Logger log;

  @Inject
  private MavenProject project;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.debug("Checking that the project does not reference a SNAPSHOT parent.");
    MavenProject parent = this.project.getParent();
    if (parent != null && IsSnapshotProject.INSTANCE.apply(parent)) {
      throw new MojoFailureException("The project cannot be released due to a SNAPSHOT parent reference!");
    }
  }
}
