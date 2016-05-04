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

@Singleton
public class ArtifactDeployer {
  @Inject
  private Deployer deployer;

  @Inject
  private RepositorySystemSession repoSession;

  @Inject
  private ReleaseMetadata metadata;

  public Collection<Artifact> deployArtifacts(Collection<Artifact> artifacts) throws DeploymentException {
    DeployRequest request = new DeployRequest();
    request.setArtifacts(artifacts);
    request.setRepository(this.metadata.getDeploymentRepository());
    DeployResult result = this.deployer.deploy(this.repoSession, request);
    return result.getArtifacts();
  }
}
