package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeploymentException;

import com.itemis.maven.aether.ArtifactDeployer;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;

/**
 * Deploys all release artifacts to the remote repository without invoking a Maven build process. Since this step cannot
 * be rolled back it is recommended to execute this step at the very last position of the processing workflow.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "deployArtifacts", description = "Deploys the release artifacts to the remote repository. It is recommended to execute this step at the very last position of the workflow since it cannot be rolled back.", requiresOnline = true)
public class DeployArtifacts implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private ArtifactDeployer deployer;
  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Deploying the release artifacts into the distribution repository");

    try {
      Collection<Artifact> deployedArtifacts = this.deployer.deployArtifacts(this.metadata.getReleaseArtifacts());
      if (!deployedArtifacts.isEmpty()) {
        this.log.debug("\tDeployed the following release artifacts to the remote repository:");
        for (Artifact a : deployedArtifacts) {
          this.log.debug("\t\t" + a);
        }
      }
    } catch (DeploymentException e) {
      throw new MojoFailureException("Unable to deploy artifacts into remote repository.", e);
    }
  }
}
