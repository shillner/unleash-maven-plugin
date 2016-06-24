package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
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
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

@ProcessingStep(id = "determineReleaseArtifacts", description = "Determines all release artifacts based on the output of the artifact-spy-plugin and stores the data in the release metadata.", requiresOnline = false)
public class DetermineReleaseArtifacts implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Determining all project release artifacts for later installation and deployment.");
    for (MavenProject p : this.reactorProjects) {
      try {
        Properties props = loadModuleArtifacts(p);
        for (String name : props.stringPropertyNames()) {
          Artifact a = new DefaultArtifact(name);
          String relativePath = props.getProperty(name);
          File artifactFile = new File(p.getBasedir(), relativePath);

          if (Objects.equal(p.getFile().getName(), relativePath)) {
            artifactFile = new File(p.getBuild().getDirectory(), "unleash/" + relativePath);
            artifactFile.getParentFile().mkdirs();
            Files.copy(p.getFile(), artifactFile);
          }

          a = a.setFile(artifactFile);
          this.metadata.addReleaseArtifact(a);

          this.log.debug("The following artifact will be installed and deployed later: " + a);
        }
      } catch (IOException e) {
        throw new MojoExecutionException(
            "Could not determine project release artifacts. Project: " + ProjectToString.INSTANCE.apply(p));
      }
    }
  }

  private Properties loadModuleArtifacts(MavenProject p) throws IOException {
    Properties props = new Properties();
    File artifactsSpyProperties = new File(p.getBuild().getDirectory() + File.separatorChar + "artifact-spy"
        + File.separatorChar + "artifacts.properties");
    props.load(new FileInputStream(artifactsSpyProperties));
    return props;
  }
}
