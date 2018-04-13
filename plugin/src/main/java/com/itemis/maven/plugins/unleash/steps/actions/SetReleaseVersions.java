package com.itemis.maven.plugins.unleash.steps.actions;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Updates the POMs of all project modules with the previously calculated release versions. This step updates project
 * versions as well as parent versions.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "setReleaseVersions", description = "Updates the POMs of all project modules with their release versions calculated previously.", requiresOnline = false)
public class SetReleaseVersions extends AbstractVersionsStep {

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Updating project modules with release versions");
    this.cachedPOMs = Maps.newHashMap();

    for (MavenProject project : this.reactorProjects) {
      Optional<Document> parsedPOM = PomUtil.parsePOM(project);
      if (parsedPOM.isPresent()) {
        this.cachedPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(project), parsedPOM.get());

        try {
          Document document = loadAndProcess(project);
          PomUtil.writePOM(document, project);
        } catch (Throwable t) {
          throw new MojoFailureException("Could not update versions for release.", t);
        }
      }
    }
  }

  @Override
  protected ReleasePhase previousReleasePhase() {
    return ReleasePhase.PRE_RELEASE;
  }

  @Override
  protected ReleasePhase currentReleasePhase() {
    return ReleasePhase.RELEASE;
  }

  @RollbackOnError
  public void rollback() throws MojoExecutionException {
    this.log.info("Rollback of release version updating for all project modules");

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tRolling back modifications on POM of module '" + ProjectToString.INSTANCE.apply(project) + "'");

      Document document = this.cachedPOMs.get(ProjectToCoordinates.EMPTY_VERSION.apply(project));
      if (document != null) {
        try {
          PomUtil.writePOM(document, project);
        } catch (Throwable t) {
          throw new MojoExecutionException(
              "Could not revert the setting of release versions after a failed release build.", t);
        }
      }
    }
  }
}
