package com.itemis.maven.aether;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.itemis.maven.plugins.cdi.logging.Logger;

/**
 * Resolves artifacts from the aether using the repository system.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Singleton
public class ArtifactResolver {
  private RepositorySystemSession repoSession;
  private LoadingCache<ArtifactCoordinates, Optional<ArtifactResult>> cache;

  @Inject
  public ArtifactResolver(RepositorySystem repoSystem, RepositorySystemSession repoSession,
      @Named("projectRepositories") List<RemoteRepository> remoteProjectRepos, Logger log) {
    this.repoSession = repoSession;
    this.cache = CacheBuilder.newBuilder()
        .build(new ArtifactCacheLoader(repoSystem, repoSession, remoteProjectRepos, log));
  }

  /**
   * Tries to resolve an artifact that is identified by its coordinates.
   *
   * @param coordinates the coordinates of the artifact to resolve.
   * @param remoteOnly {@code true} if the artifact resolving shall be performed remotely only which means that the
   *          artifact will only be delivered if it is present in any of the remote repositories.
   * @return the artifact as a file.
   */
  public Optional<File> resolve(ArtifactCoordinates coordinates, boolean remoteOnly) {
    File f = null;
    try {
      Optional<ArtifactResult> result = this.cache
          .get(new ArtifactCoordinates(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(),
              MoreObjects.firstNonNull(coordinates.getType(), "jar"), coordinates.getClassifier()));
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
