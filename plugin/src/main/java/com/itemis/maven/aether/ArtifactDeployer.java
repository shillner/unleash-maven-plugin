package com.itemis.maven.aether;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.impl.Deployer;

import com.itemis.maven.plugins.unleash.ReleaseMetadata;

/**
 * Deploys artifacts into the remote Maven repositories.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Singleton
public class ArtifactDeployer {
  @Inject
  private Deployer deployer;

  @Inject
  private RepositorySystemSession repoSession;

  @Inject
  private ReleaseMetadata metadata;

  /**
   * Deploys the given artifacts to the configured remote Maven repositories.
   *
   * @param artifacts the artifacts to deploy.
   * @return the artifacts that have been deployed successfully.
   * @throws DeploymentException if anything goes wrong during the deployment process.
   */
  public Collection<Artifact> deployArtifacts(Collection<Artifact> artifacts) throws DeploymentException {
    DeployRequest request = new DeployRequest();
    request.setArtifacts(artifacts);
    request.setRepository(this.metadata.getDeploymentRepository());
    DeployResult result = this.deployer.deploy(this.repoSession, request);
    return result.getArtifacts();
  }
}
