package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeploymentException;

import com.google.common.collect.Sets;
import com.itemis.maven.aether.ArtifactDeployer;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

@ProcessingStep(@Goal(name = "perform", stepNumber = 90))
public class DeployArtifacts implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private ArtifactDeployer deployer;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.info("Deploying the release artifacts into the distribution repository");

    Collection<Artifact> artifactsToDeploy = Sets.newHashSet();
    for (MavenProject p : this.reactorProjects) {
      try {
        Properties props = loadModuleArtifacts(p);
        for (String name : props.stringPropertyNames()) {
          Artifact a = new DefaultArtifact(name);
          a = a.setFile(new File(p.getBasedir(), props.getProperty(name)));
          artifactsToDeploy.add(a);
        }
      } catch (IOException e) {
        throw new MojoExecutionException(
            "Could not determine project artifacts to deploy. Project: " + ProjectToString.INSTANCE.apply(p));
      }
    }

    try {
      this.deployer.deployArtifacts(artifactsToDeploy);
    } catch (DeploymentException e) {
      throw new MojoFailureException("Unable to deploy artifacts into remote repository.", e);
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
