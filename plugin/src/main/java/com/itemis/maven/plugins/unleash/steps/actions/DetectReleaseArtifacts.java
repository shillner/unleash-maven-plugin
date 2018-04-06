package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import com.google.common.base.Objects;
import com.google.common.io.Files;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Detects all releaseArtifacts from the output of the artifact-spy-plugin that had been smuggled into the build
 * process. This is necessary in order to install and deploy all artifacts after the actual build process instead of
 * doing this during the build process. This ensures working rollback mechanisms in case of a failure.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "detectReleaseArtifacts", description = "Detects all release artifacts based on the output of the artifact-spy-plugin and stores the data in the release metadata for later installation and deployment.", requiresOnline = false)
public class DetectReleaseArtifacts implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private MavenProject project;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("unleashOutputFolder")
  private File unleashOutputFolder;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info(
        "Detecting all release artifacts that have been produced during the release build for later installation and deployment.");

    for (MavenProject p : this.reactorProjects) {
      if (wasFixedVersion(p)) {
        continue;
      }

      try {
        Properties props = loadModuleArtifacts(p);
        for (String name : props.stringPropertyNames()) {
          Artifact a = new DefaultArtifact(name);
          String relativePath = props.getProperty(name);
          File artifactFile = new File(p.getBasedir(), relativePath);

          // in case of pom artifacts the poms are copied to a different location to ensure we upload the correct
          // version of the pom since the pom evolves during the release build.
          if (Objects.equal(p.getFile().getName(), relativePath)) {
            relativePath = this.project.getBasedir().toURI().relativize(p.getFile().toURI()).toString();
            artifactFile = new File(this.unleashOutputFolder, relativePath);
            artifactFile.getParentFile().mkdirs();

            // see https://github.com/shillner/unleash-maven-plugin/issues/98
            if (p.getFile().exists()) {
              Files.copy(p.getFile(), artifactFile);
            }
          }

          a = a.setFile(artifactFile);
          this.metadata.addReleaseArtifact(a);

          this.log.debug("\t\tThe following artifact will be installed and deployed later: " + a);
        }
      } catch (IOException e) {
        throw new MojoExecutionException(
            "Could not determine project release artifacts. Project: " + ProjectToString.INSTANCE.apply(p), e);
      }
    }
  }

  private Properties loadModuleArtifacts(MavenProject p) throws MojoExecutionException, MojoFailureException {
    Properties props = new Properties();
    // TODO outsource name of file to metadata!
    File artifactsSpyProperties = new File(p.getBuild().getDirectory(), "artifact-spy/artifacts.properties");
    if (artifactsSpyProperties.exists() && artifactsSpyProperties.isFile()) {
      try {
        this.log.debug("\tLoading artifact-spy output of module '" + ProjectToString.INSTANCE.apply(p) + "' from "
            + artifactsSpyProperties.getAbsolutePath());
        props.load(new FileInputStream(artifactsSpyProperties));
      } catch (Exception e) {
        throw new MojoExecutionException(
            "Unable to load artifact-spy output file from " + artifactsSpyProperties.getAbsolutePath(), e);
      }
    } else {
      throw new MojoFailureException(
          "Could not find artifact-spy output file containing all project artifacts. File was expected at "
              + artifactsSpyProperties.getAbsolutePath());
    }
    return props;
  }

  private boolean wasFixedVersion(MavenProject p) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(p.getGroupId(), p.getArtifactId());
    String preReleaseVersion = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE).getVersion();
    String releaseVersion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();

    if (Objects.equal(preReleaseVersion, releaseVersion)) {
      return true;
    }
    return false;
  }
}
