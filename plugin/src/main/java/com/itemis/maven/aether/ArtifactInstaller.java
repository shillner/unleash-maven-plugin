package com.itemis.maven.aether;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;

/**
 * Installs artifacts into the local Maven repository.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Singleton
public class ArtifactInstaller {
  @Inject
  private RepositorySystemSession repoSession;

  @Inject
  private Installer installer;

  /**
   * Installs the given artifacts into the local Maven repository.
   * 
   * @param artifacts the artifacts to install.
   * @return the artifacts that have been installed successfully.
   * @throws InstallationException if anything goes wrong during the installation process.
   */
  public Collection<Artifact> installArtifacts(Collection<Artifact> artifacts) throws InstallationException {
    InstallRequest request = new InstallRequest();
    request.setArtifacts(artifacts);
    InstallResult result = this.installer.install(this.repoSession, request);
    return result.getArtifacts();
  }
}
