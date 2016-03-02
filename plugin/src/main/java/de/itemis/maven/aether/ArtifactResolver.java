package de.itemis.maven.aether;

import java.io.File;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

public final class ArtifactResolver {
  private RepositorySystem repoSystem;
  private RepositorySystemSession repoSession;
  private List<RemoteRepository> remoteProjectRepos;
  private MavenLogWrapper log;
  private LoadingCache<ArtifactCoordinates, Optional<ArtifactResult>> cache;

  public ArtifactResolver(RepositorySystem repoSystem, RepositorySystemSession repoSession,
      List<RemoteRepository> remoteProjectRepos, MavenLogWrapper log) {
    this.repoSystem = repoSystem;
    this.repoSession = repoSession;
    this.remoteProjectRepos = remoteProjectRepos;
    this.log = log;
    this.cache = CacheBuilder.newBuilder()
        .build(new ArtifactCacheLoader(repoSystem, repoSession, remoteProjectRepos, log));
  }

  public Optional<File> resolve(String groupId, String artifactId, String version, Optional<String> type,
      Optional<String> classifier, boolean remoteOnly) {
    File f = null;
    try {
      Optional<ArtifactResult> result = this.cache
          .get(new ArtifactCoordinates(groupId, artifactId, version, type.or("jar"), classifier.orNull()));
      if (result.isPresent()) {
        f = getRawArtifact(result.get(), remoteOnly);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t.getMessage(), t);
    }
    return Optional.fromNullable(f);
  }

  private File getRawArtifact(ArtifactResult artifactResult, boolean remoteOnly) {
    File result = null;
    Artifact artifact = artifactResult.getArtifact();
    if (artifact != null) {
      if (remoteOnly) {
        if (!Objects.equal(artifactResult.getRepository().getId(), this.repoSession.getLocalRepository().getId())) {
          result = artifact.getFile();
        }
      } else {
        result = artifact.getFile();
      }
    }
    return result;
  }
}
