package de.itemis.maven.aether.cache;

import org.apache.maven.plugins.annotations.Component;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import de.itemis.maven.aether.ArtifactCoordinates;

public class ArtifactCache {
  private static ArtifactCache instance;
  private Cache<ArtifactCoordinates, Optional<ArtifactResult>> cache;

  @Component
  public RepositorySystem repoSystem;

  public ArtifactCache() {
    System.out.println(this.repoSystem);
    // this.cache = CacheBuilder.newBuilder().build(new ArtifactCacheLoader());
  }

  public static ArtifactCache getInstance() {
    if (instance == null) {
      instance = new ArtifactCache();
    }
    return instance;
  }

  // TODO add methods for local or remote resolving
}
