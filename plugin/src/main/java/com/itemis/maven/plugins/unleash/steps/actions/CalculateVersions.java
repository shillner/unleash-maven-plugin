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
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;
import com.itemis.maven.plugins.unleash.util.VersionUpgradeStrategy;
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
  @Inject
  private VersionUpgradeStrategy upgradeStrategy;
  @Inject
  @Named("preserveFixedModuleVersions")
  private boolean preserveFixedModuleVersions;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Calculating required versions for all modules.");

    for (MavenProject project : this.reactorProjects) {
      this.log.info("\tVersions of module " + ProjectToString.EXCLUDE_VERSION.apply(project) + ":");

      ArtifactCoordinates preReleaseCoordinates = this.metadata
          .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId()).get(ReleasePhase.PRE_RELEASE);
      this.log.info("\t\t" + ReleasePhase.PRE_RELEASE + " = " + preReleaseCoordinates.getVersion());

      Optional<Prompter> prompterToUse = this.settings.isInteractiveMode() ? Optional.of(this.prompter)
          : Optional.<Prompter> absent();

      String releaseVersion = calculateReleaseVersion(project.getVersion(), prompterToUse);
      ArtifactCoordinates releaseCoordinates = new ArtifactCoordinates(project.getGroupId(), project.getArtifactId(),
          releaseVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(releaseCoordinates, ReleasePhase.RELEASE);
      this.log.info("\t\t" + ReleasePhase.RELEASE + " = " + releaseVersion);

      String nextDevVersion = calculateDevelopmentVersion(project.getVersion(), prompterToUse);
      ArtifactCoordinates postReleaseCoordinates = new ArtifactCoordinates(project.getGroupId(),
          project.getArtifactId(), nextDevVersion, PomUtil.ARTIFACT_TYPE_POM);
      this.metadata.addArtifactCoordinates(postReleaseCoordinates, ReleasePhase.POST_RELEASE);
      this.log.info("\t\t" + ReleasePhase.POST_RELEASE + " = " + nextDevVersion);
    }
  }

  private String calculateReleaseVersion(String version, Optional<Prompter> prompter) {

    if (!MavenVersionUtil.isSnapshot(version) && this.preserveFixedModuleVersions) {
      return version;
    }
    return ReleaseUtil.getReleaseVersion(version, Optional.fromNullable(this.defaultReleaseVersion), prompter);
  }

  private String calculateDevelopmentVersion(String version, Optional<Prompter> prompter) {
    if (!MavenVersionUtil.isSnapshot(version) && this.preserveFixedModuleVersions) {
      return version;
    }
    return ReleaseUtil.getNextDevelopmentVersion(version, Optional.fromNullable(this.defaultDevelopmentVersion),
        prompter, this.upgradeStrategy);
  }
}
