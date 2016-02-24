package de.itemis.maven.plugins.unleash.actions;

import com.google.common.collect.Collections2;

import de.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProjectPredicate;

/**
 * Checks that at least one of the projects is releasable, which means that at least one of the projects must have a
 * snapshot version assigned.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public class CheckReleasable extends AbstractProcessingAction {

  @Override
  public void execute() {
    getLog().debug("Checking that at least one of the reactor projects has a SNAPSHOT version assigned.");
    boolean hasSnapshotProjects = !Collections2.filter(getReactorProjects(), new IsSnapshotProjectPredicate())
        .isEmpty();

    if (!hasSnapshotProjects) {
      String errorTitle = "There are no snapshot projects that could be released!";
      getLog().error(errorTitle);
      getLog().error("The reactor project list must contain at least one project with a SNAPSHOT version assigned.");
      throw new IllegalStateException(errorTitle);
    }
  }
}
