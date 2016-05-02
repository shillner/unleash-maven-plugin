package com.itemis.maven.aether;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.slf4j.Slf4jLoggerFactory;

import com.google.common.collect.Sets;

@Singleton
public class ArtifactInstaller {
  @Inject
  public RepositorySystemSession repoSession;
  private Installer installer;

  public ArtifactInstaller() {
    DefaultInstaller installer = new DefaultInstaller();
    installer.setFileProcessor(new DefaultFileProcessor());
    Slf4jLoggerFactory loggerFactory = new Slf4jLoggerFactory();
    installer.setLoggerFactory(loggerFactory);
    Collection<MetadataGeneratorFactory> metadataGeneratorFactories = Sets
        .newHashSet(new SnapshotMetadataGeneratorFactory(), new VersionsMetadataGeneratorFactory());
    installer.setMetadataGeneratorFactories(metadataGeneratorFactories);
    DefaultRepositoryEventDispatcher repositoryEventDispatcher = new DefaultRepositoryEventDispatcher();
    repositoryEventDispatcher.setLoggerFactory(loggerFactory);
    repositoryEventDispatcher.setRepositoryListeners(Collections.<RepositoryListener> emptyList());
    installer.setRepositoryEventDispatcher(repositoryEventDispatcher);
    installer.setSyncContextFactory(new DefaultSyncContextFactory());
    this.installer = installer;
  }

  public Collection<Artifact> installArtifacts(Collection<Artifact> artifacts) throws InstallationException {
    InstallRequest request = new InstallRequest();
    request.setArtifacts(artifacts);
    InstallResult result = this.installer.install(this.repoSession, request);
    return result.getArtifacts();
  }
}
