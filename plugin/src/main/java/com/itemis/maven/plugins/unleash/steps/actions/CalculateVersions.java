package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;

import com.google.common.base.Optional;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Calculates the versions for all modules of the project used during the release build.<br>
 * This step applies several strategies for version calculation such as prompting the user or checking globally defined
 * versions.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "prepareVersions", description = "Calculates all required versions for each module of the project such as release and development version (applies several strategies for version calculation such as user prompting).", requiresOnline = false)
public class CalculateVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
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
  @Inject
  private Settings settings;
  @Inject
  private Prompter prompter;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Calculating required versions for all modules.");

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tVersions of module " + ProjectToString.EXCLUDE_VERSION.apply(project) + ":");

      ArtifactCoordinates preReleaseCoordinates = this.metadata
          .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId()).get(ReleasePhase.PRE_RELEASE);
      this.log.debug("\t\t" + ReleasePhase.PRE_RELEASE + " = " + preReleaseCoordinates.getVersion());

      Optional<Prompter> prompterToUse = this.settings.isInteractiveMode() ? Optional.of(this.prompter)
          : Optional.<Prompter> absent();

      String releaseVersion = ReleaseUtil.getReleaseVersion(project.getVersion(),
          Optional.fromNullable(this.defaultReleaseVersion), prompterToUse);
      ArtifactCoordinates releaseCoordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          releaseVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(releaseCoordinates, ReleasePhase.RELEASE);
      this.log.debug("\t\t" + ReleasePhase.RELEASE + " = " + releaseVersion);

      String nextDevVersion = ReleaseUtil.getNextDevelopmentVersion(releaseVersion,
          Optional.fromNullable(this.defaultDevelopmentVersion), prompterToUse);
      ArtifactCoordinates postReleaseCoordinates = new ArtifactCoordinates(project.getGroupId(),
          project.getArtifactId(), nextDevVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(postReleaseCoordinates, ReleasePhase.POST_RELEASE);
      this.log.debug("\t\t" + ReleasePhase.POST_RELEASE + " = " + nextDevVersion);
    }
  }
}
