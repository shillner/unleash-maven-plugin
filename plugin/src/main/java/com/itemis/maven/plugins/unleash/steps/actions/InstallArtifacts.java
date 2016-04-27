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

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@ProcessingStep(@Goal(name = "perform", stepNumber = 80))
public class InstallArtifacts implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  private MavenProject project;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.info("Installing the release artifacts into the local repository");

    // TODO implement!
    for (MavenProject p : this.reactorProjects) {
      Properties props = new Properties();
      File artifactsSpyProperties = new File(p.getBuild().getDirectory() + File.separatorChar + "artifact-spy"
          + File.separatorChar + "artifacts.properties");
      try {
        props.load(new FileInputStream(artifactsSpyProperties));
        for (String name : props.stringPropertyNames()) {
          Artifact a = new DefaultArtifact(name);
          a = a.setFile(new File(p.getBasedir(), props.getProperty(name)));

          System.out.println("Artifact: " + a + " => " + a.getFile().getAbsolutePath());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    throw new RuntimeException();

    // InvocationRequest request = new DefaultInvocationRequest();
    // request.setPomFile(this.project.getFile());
    // request.setGoals(Collections.singletonList("install:install"));
  }
}
