package com.itemis.maven.plugins.unleash.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;

import com.google.common.collect.Collections2;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProjectPredicate;

/***
 * Checks that at least one of the projects is releasable,which means that at least one of the projects must have
 * a*snapshot version assigned.**
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
@ProcessingStep(@Goal(name = "perform", stepNumber = 1))
public class CheckReleasable implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute() {
    this.log.debug("Checking that at least one of the reactor projects has a SNAPSHOT version assigned.");
    boolean hasSnapshotProjects = !Collections2.filter(this.reactorProjects, IsSnapshotProjectPredicate.INSTANCE)
        .isEmpty();

    if (!hasSnapshotProjects) {
      String errorTitle = "There are no snapshot projects that could be released!";
      this.log.error(errorTitle);
      this.log.error("The reactor project list must contain at least one project with a SNAPSHOT version assigned.");
      throw new IllegalStateException(errorTitle);
    }
  }
}
