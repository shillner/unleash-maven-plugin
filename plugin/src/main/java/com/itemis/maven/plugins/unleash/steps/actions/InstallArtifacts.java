package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.installation.InstallationException;

import com.google.common.collect.Collections2;
import com.itemis.maven.aether.ArtifactInstaller;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.functions.AetherToMavenArtifact;

@ProcessingStep(id = "installArtifacts", description = "Installs the release artifacts into the local repository.", requiresOnline = false)
public class InstallArtifacts implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  @Named("local")
  private ArtifactRepository LocalRepository;

  @Inject
  private ArtifactInstaller installer;

  @Inject
  private ReleaseMetadata metadata;

  private Collection<Artifact> installedArtifacts;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.info("Installing the release artifacts into the local repository");

    try {
      this.installedArtifacts = this.installer.installArtifacts(this.metadata.getReleaseArtifacts());
    } catch (InstallationException e) {
      throw new MojoFailureException("Unable to install artifacts into local repository.", e);
    }
  }

  @RollbackOnError
  public void rollback() {
    this.log.info("Rolling back local artifact installatio due to a processing exception.");

    Collection<org.apache.maven.artifact.Artifact> artifacts = Collections2.transform(this.installedArtifacts,
        AetherToMavenArtifact.INSTANCE);
    for (org.apache.maven.artifact.Artifact artifact : artifacts) {
      File localArtifact = new File(this.LocalRepository.getBasedir(), this.LocalRepository.pathOf(artifact));
      File localArtifactDirectory = localArtifact.getParentFile();
      try {
        this.log.debug("Deleting locally installed artifact (parent directory): " + artifact);
        FileUtils.deleteDirectory(localArtifactDirectory);
      } catch (IOException e) {
        // do not fail in order to ensure that the other executions can also be rolled-back
        this.log.error(
            "Error rolling back artifact installation. Could not delete locally installed artifact: " + artifact, e);
      }
    }
  }
}
