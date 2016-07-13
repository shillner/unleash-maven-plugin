package com.itemis.maven.plugins.unleash.steps.checks;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.aether.ArtifactResolver;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

/**
 * Checks the aether for already released artifacts. This check comprises only modules that are scheduled for release
 * and leaves out others.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "checkAether", description = "Checks the aether for already released artifacts. The goal is to ensure that the artifacts produced by this release build can be deployed safely to the aether.", requiresOnline = true)
public class CheckAether implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private ArtifactResolver artifactResolver;
  @Inject
  @Named("allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Checking aether for already released artifacts of modules that are scheduled for release.");
    this.log.debug(
        "\t=> If any of the modules had already been released with the corresponding release version, the release build will fail fast at this point.");

    Collection<MavenProject> snapshotProjects = Collections2.filter(this.reactorProjects, IsSnapshotProject.INSTANCE);

    List<MavenProject> alreadyReleasedProjects = Lists.newArrayList();
    for (MavenProject p : snapshotProjects) {
      this.log.debug("\tChecking module '" + ProjectToString.INSTANCE.apply(p) + "'");
      ArtifactCoordinates calculatedCoordinates = this.metadata
          .getArtifactCoordinatesByPhase(p.getGroupId(), p.getArtifactId()).get(ReleasePhase.RELEASE);
      if (isReleased(calculatedCoordinates.getGroupId(), calculatedCoordinates.getArtifactId(),
          calculatedCoordinates.getVersion())) {
        alreadyReleasedProjects.add(p);
      }
    }

    if (!alreadyReleasedProjects.isEmpty()) {
      this.log.error("\tThe following projects are already present in one of your remote repositories:");
      for (MavenProject p : alreadyReleasedProjects) {
        this.log.error("\t\t" + ProjectToString.INSTANCE.apply(p));
      }
      throw new IllegalStateException(
          "Some of the reactor projects have already been released. Please check your repositories!");
    }
  }

  private boolean isReleased(String groupId, String artifactId, String version) {
    Optional<File> pom = this.artifactResolver.resolve(groupId, artifactId, version,
        Optional.of(PomUtil.ARTIFACT_TYPE_POM), Optional.<String> absent(), this.allowLocalReleaseArtifacts);
    return pom.isPresent();
  }
}
