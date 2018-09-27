package com.itemis.maven.aether;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.repository.RemoteRepository;

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
  @Inject
  @Named("additionalDeployemntRepositories")
  private Set<RemoteRepository> additonalDeploymentRepositories;

  /**
   * Deploys the given artifacts to the configured remote Maven repositories.
   *
   * @param artifacts the artifacts to deploy.
   * @return the artifacts that have been deployed successfully.
   * @throws DeploymentException if anything goes wrong during the deployment process.
   */
  public Collection<Artifact> deployArtifacts(Collection<Artifact> artifacts) throws DeploymentException {
    Collection<Artifact> result = deploy(artifacts, this.metadata.getDeploymentRepository());
    for (RemoteRepository repo : this.additonalDeploymentRepositories) {
      deploy(artifacts, repo);
    }
    return result;
  }

  private Collection<Artifact> deploy(Collection<Artifact> artifacts, RemoteRepository repo)
      throws DeploymentException {
    DeployRequest request = new DeployRequest();
    request.setArtifacts(artifacts);
    request.setRepository(repo);
    DeployResult result = this.deployer.deploy(this.repoSession, request);
    return result.getArtifacts();
  }
}
