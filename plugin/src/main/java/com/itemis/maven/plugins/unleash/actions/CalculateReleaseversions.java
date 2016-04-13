package com.itemis.maven.plugins.unleash.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;

@ProcessingStep(@Goal(name = "perform", stepNumber = 20))
public class CalculateReleaseversions implements CDIMojoProcessingStep {
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private ReleaseMetadata metadata;

  @Named("releaseVersion")
  private String defaultReleaseVersion;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    for (MavenProject project : this.reactorProjects) {
      ArtifactCoordinates coordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          project.getVersion(), PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(coordinates, ReleasePhase.PRE);

      // TODO add user questions for new release versions here!
      String releaseVersion = ReleaseUtil.getReleaseVersion(project.getVersion(), this.defaultReleaseVersion);
      ArtifactCoordinates releaseCoordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          releaseVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(releaseCoordinates, ReleasePhase.POST);
    }
  }
}
