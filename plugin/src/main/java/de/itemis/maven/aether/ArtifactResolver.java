package de.itemis.maven.aether;

import java.io.File;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

public final class ArtifactResolver {
  private Cache<ArtifactCoordinates, Artifact> artifactCache;
  private RepositorySystem repoSystem;
  private RepositorySystemSession repoSession;
  private List<RemoteRepository> remoteProjectRepos;
  private MavenLogWrapper log;

  public ArtifactResolver(RepositorySystem repoSystem, RepositorySystemSession repoSession,
      List<RemoteRepository> remoteProjectRepos, MavenLogWrapper log) {
    this.repoSystem = repoSystem;
    this.repoSession = repoSession;
    this.remoteProjectRepos = remoteProjectRepos;
    this.log = log;
  }

  private void setupCache() {
    // this.artifactCache = CacheBuilder.newBuilder().build(loader);
  }

  public Optional<File> resolveArtifact(String groupId, String artifactId, String version, Optional<String> type,
      Optional<String> classifier) {
    Optional<File> result;

    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier.orNull(), type.orNull(), version);

    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(this.remoteProjectRepos);

    try {
      // TODO for loal-only see LocalArtifactRequest
      ArtifactResult artifactResult = this.repoSystem.resolveArtifact(this.repoSession, artifactRequest);
      artifact = artifactResult.getArtifact();

      if (artifact != null) {
        // FIXME create other methods that resolve again local or remotes only! -> was just for testing
        if (!Objects.equal(artifactResult.getRepository().getId(), this.repoSession.getLocalRepository().getId())) {
          this.log.info("Artifact available also in remote repos!");
          result = Optional.fromNullable(artifact.getFile());
        } else {
          this.log.info("Artifact available only in local repo!");
          result = Optional.absent();
        }
      } else {
        result = Optional.absent();
      }
    } catch (ArtifactResolutionException e) {
      this.log.debug(e.getMessage());
      // must not throw the error or log as an error since this is an expected behavior
      result = Optional.absent();
    }

    return result;
  }
}
