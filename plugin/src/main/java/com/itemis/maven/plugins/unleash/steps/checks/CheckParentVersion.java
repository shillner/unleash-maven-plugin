package com.itemis.maven.plugins.unleash.steps.checks;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

@ProcessingStep(id = "checkParentVersion", description = "Checks that the project does not reference a SNAPSHOT parent.")
public class CheckParentVersion implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.debug("Checking that the project does not reference a SNAPSHOT parent.");
    MavenProject parent = this.project.getParent();
    if (parent != null && IsSnapshotProject.INSTANCE.apply(parent)) {
      throw new MojoFailureException("The project cannot be released due to a SNAPSHOT parent reference!");
    }
  }
}
