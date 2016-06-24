package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.Collections2;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

/**
 * Checks that at least one of the projects is releasable, which means that at least one of the projects must have
 * a snapshot version assigned.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
@ProcessingStep(id = "checkProjectVersions", description = "Checks that at least one of the projects as a SNAPSHOT version assigned and is thus releasable.", requiresOnline = false)
public class CheckProjectVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.debug("Checking that at least one of the reactor projects has a SNAPSHOT version assigned.");
    boolean hasSnapshotProjects = !Collections2.filter(this.reactorProjects, IsSnapshotProject.INSTANCE).isEmpty();

    if (!hasSnapshotProjects) {
      String errorTitle = "There are no snapshot projects that could be released!";
      for (MavenProject p : this.reactorProjects) {
        this.log.info(ProjectToString.INSTANCE.apply(p));
      }
      this.log.error(errorTitle);
      this.log.error("The reactor project list must contain at least one project with a SNAPSHOT version assigned.");
      throw new IllegalStateException(errorTitle);
    }
  }
}
