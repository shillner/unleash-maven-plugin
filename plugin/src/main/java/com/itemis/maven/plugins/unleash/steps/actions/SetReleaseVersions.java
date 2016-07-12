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

@ProcessingStep(id = "setReleaseVersions", description = "Updates all projects with their release versions calculated previously.", requiresOnline = false)
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
    this.cachedPOMs = Maps.newHashMap();
    for (MavenProject project : this.reactorProjects) {
      this.cachedPOMs.put(new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          MavenProject.EMPTY_PROJECT_VERSION, project.getPackaging()), PomUtil.parsePOM(project));

      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document);
        setParentVersion(project, document);
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for release.", t);
      }
    }
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVerion = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE).getVersion();
    String newVersion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
    this.log.info("Update of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' [" + oldVerion
        + " => " + newVersion + "]");
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
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
        this.log.info("Update of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
      }
    }
  }

  @RollbackOnError
  public void rollback() throws MojoExecutionException {
    for (MavenProject project : this.reactorProjects) {
      Document document = this.cachedPOMs.get(new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          MavenProject.EMPTY_PROJECT_VERSION, project.getPackaging()));
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
