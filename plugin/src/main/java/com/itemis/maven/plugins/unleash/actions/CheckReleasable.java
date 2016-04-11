package com.itemis.maven.plugins.unleash.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;

import com.google.common.collect.Collections2;
import com.itemis.maven.plugins.cdi.InjectableCdiMojo;
import com.itemis.maven.plugins.cdi.annotations.MojoExecution;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProjectPredicate;

/***
 * Checks that at least one of the projects is releasable,which means that at least one of the projects must have
 * a*snapshot version assigned.**
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
@MojoExecution(name = "perform", order = 1)
public class CheckReleasable implements InjectableCdiMojo {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute() {
    this.log.debug("Checking that at least one of the reactor projects has a SNAPSHOT version assigned.");
    boolean hasSnapshotProjects = !Collections2.filter(this.reactorProjects, new IsSnapshotProjectPredicate())
        .isEmpty();

    if (!hasSnapshotProjects) {
      String errorTitle = "There are no snapshot projects that could be released!";
      this.log.error(errorTitle);
      this.log.error("The reactor project list must contain at least one project with a SNAPSHOT version assigned.");
      throw new IllegalStateException(errorTitle);
    }

    System.out.println("******************************");
    System.out.println("REVISION: " + this.metadata.getPreReleaseScmRevision());
  }
}