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

import com.google.common.base.Optional;

import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

public final class ArtifactResolver {
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

  // resolves artifacts from remote repos only! -> that's exacly the right thing here but
  // TODO: how can we resolve local-only artifacts? -> Artifacts that are only installed but not deployed
  public Optional<File> resolveArtifact(String groupId, String artifactId, String version, Optional<String> type,
      Optional<String> classifier) {
    Optional<File> result;

    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier.orNull(), type.orNull(), version);

    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(this.remoteProjectRepos);

    try {
      ArtifactResult artifactResult = this.repoSystem.resolveArtifact(this.repoSession, artifactRequest);
      artifact = artifactResult.getArtifact();
      if (artifact != null) {
        result = Optional.fromNullable(artifact.getFile());
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
