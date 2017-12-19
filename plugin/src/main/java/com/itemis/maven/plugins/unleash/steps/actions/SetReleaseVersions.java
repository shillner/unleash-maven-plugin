package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
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
public class SetReleaseVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  private Map<ArtifactCoordinates, Document> cachedPOMs;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Updating project modules with release versions");
    this.cachedPOMs = Maps.newHashMap();

    for (MavenProject project : this.reactorProjects) {
      Optional<Document> parsedPOM = PomUtil.parsePOM(project);
      if (parsedPOM.isPresent()) {
        this.cachedPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(project), parsedPOM.get());

        try {
          // parse again to not modify the cached object
          Document document = PomUtil.parsePOM(project).get();
          setProjectVersion(project, document);
          setParentVersion(project, document);
          PomUtil.writePOM(document, project);
        } catch (Throwable t) {
          throw new MojoFailureException("Could not update versions for release.", t);
        }
      }
    }
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVerion = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE).getVersion();
    String newVersion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();
    this.log.debug("\tUpdate of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' ["
        + oldVerion + " => " + newVersion + "]");
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
  }

  private void setParentVersion(MavenProject project, Document document) {
    Parent parent = project.getModel().getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE);
      ArtifactCoordinates newCoordinates = coordinatesByPhase.get(ReleasePhase.RELEASE);

      // null indicates that the parent is not part of the reactor projects since no release version had been calculated
      // for it
      if (newCoordinates != null) {
        this.log.debug("\tUpdate of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
      }
    }
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
