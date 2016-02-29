package de.itemis.maven.aether.cache;

import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;

import de.itemis.maven.aether.ArtifactCoordinates;
import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

class ArtifactCacheLoader extends CacheLoader<ArtifactCoordinates, Optional<ArtifactResult>> {
  private RepositorySystem repoSystem;
  private RepositorySystemSession repoSession;
  private List<RemoteRepository> remoteProjectRepos;
  private MavenLogWrapper log;

  public ArtifactCacheLoader(RepositorySystem repoSystem, RepositorySystemSession repoSession,
      List<RemoteRepository> remoteProjectRepos, MavenLogWrapper log) {
    this.repoSystem = repoSystem;
    this.repoSession = repoSession;
    this.remoteProjectRepos = remoteProjectRepos;
    this.log = log;
  }

  @Override
  public Optional<ArtifactResult> load(ArtifactCoordinates coordinates) throws Exception {
    Artifact artifact = new DefaultArtifact(coordinates.toString());

    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(this.remoteProjectRepos);

    ArtifactResult artifactResult;
    try {
      artifactResult = this.repoSystem.resolveArtifact(this.repoSession, artifactRequest);
    } catch (ArtifactResolutionException e) {
      // must not throw the error or log as an error since this is an expected behavior
      artifactResult = null;
    }

    return Optional.fromNullable(artifactResult);
  }
}
