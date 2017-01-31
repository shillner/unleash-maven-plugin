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
   * @return the result of the artifact resolution containing the file and the id of the repository from which is has
   *         been resolved.
   */
  public Optional<ResolutionResult> resolve(ArtifactCoordinates coordinates, boolean remoteOnly) {
    ResolutionResult r = null;
    try {
      Optional<ArtifactResult> result = this.cache
          .get(new ArtifactCoordinates(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(),
              MoreObjects.firstNonNull(coordinates.getType(), "jar"), coordinates.getClassifier()));
      if (result.isPresent()) {
        r = getResolutionResult(result.get(), remoteOnly);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t.getMessage(), t);
    }
    return Optional.fromNullable(r);
  }

  private ResolutionResult getResolutionResult(ArtifactResult artifactResult, boolean remoteOnly) {
    ResolutionResult result = null;
    Artifact artifact = artifactResult.getArtifact();
    if (artifact != null) {
      String repositoryId = artifactResult.getRepository().getId();
      if (remoteOnly) {
        if (!Objects.equal(repositoryId, this.repoSession.getLocalRepository().getId())) {
          result = new ResolutionResult(artifact.getFile(), repositoryId);
        }
      } else {
        result = new ResolutionResult(artifact.getFile(), repositoryId);
      }
    }
    return result;
  }

  public static class ResolutionResult {
    private File file;
    private String repositoryId;

    public ResolutionResult(File f, String repositoryId) {
      this.file = f;
      this.repositoryId = repositoryId;
    }

    public File getFile() {
      return this.file;
    }

    public String getRepositoryId() {
      return this.repositoryId;
    }
  }
}
