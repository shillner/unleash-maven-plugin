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
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

@ProcessingStep(id = "checkAether", description = "Checks the Aether for already released artifacts. The goal is to ensure that the artifacts produced by this release build can be deployed safely to the aether.")
public class CheckAether implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

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
  public void execute() throws MojoExecutionException, MojoFailureException {
    Collection<MavenProject> snapshotProjects = Collections2.filter(this.reactorProjects, IsSnapshotProject.INSTANCE);

    List<MavenProject> alreadyReleasedProjects = Lists.newArrayList();
    for (MavenProject p : snapshotProjects) {
      ArtifactCoordinates calculatedCoordinates = this.metadata
          .getArtifactCoordinatesByPhase(p.getGroupId(), p.getArtifactId()).get(ReleasePhase.RELEASE);
      if (isReleased(calculatedCoordinates.getGroupId(), calculatedCoordinates.getArtifactId(),
          calculatedCoordinates.getVersion())) {
        alreadyReleasedProjects.add(p);
      }
    }

    if (!alreadyReleasedProjects.isEmpty()) {
      this.log.error("The following projects are already present in one of your remote repositories:");
      for (MavenProject p : alreadyReleasedProjects) {
        this.log.error("\t" + ProjectToString.INSTANCE.apply(p));
      }
      this.log.error("");
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
