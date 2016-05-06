package com.itemis.maven.plugins.unleash.steps.actions;

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
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

@ProcessingStep(@Goal(name = "perform", stepNumber = 20))
public class CalculateVersions implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private ReleaseMetadata metadata;

  @Inject
  @Named("releaseVersion")
  private String defaultReleaseVersion;

  @Inject
  @Named("developmentVersion")
  private String defaultDevelopmentVersion;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.info("Calculating versions for all modules.");

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("Versions of project " + ProjectToString.INSTANCE.apply(project) + ":");

      ArtifactCoordinates coordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          project.getVersion(), PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(coordinates, ReleasePhase.PRE_RELEASE);
      this.log.debug("\t" + ReleasePhase.PRE_RELEASE + " = " + coordinates.getVersion());

      // TODO add user questions for new release versions here!
      String releaseVersion = ReleaseUtil.getReleaseVersion(project.getVersion(), this.defaultReleaseVersion);
      ArtifactCoordinates releaseCoordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          releaseVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(releaseCoordinates, ReleasePhase.RELEASE);
      this.log.debug("\t" + ReleasePhase.RELEASE + " = " + releaseVersion);

      String nextDevVersion = ReleaseUtil.getNextDevelopmentVersion(project.getVersion(),
          this.defaultDevelopmentVersion);
      ArtifactCoordinates postReleaseCoordinates = new ArtifactCoordinates(project.getGroupId(),
          project.getArtifactId(), nextDevVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(postReleaseCoordinates, ReleasePhase.POST_RELEASE);
      this.log.debug("\t" + ReleasePhase.POST_RELEASE + " = " + nextDevVersion);
    }
  }
}
