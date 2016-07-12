package com.itemis.maven.plugins.unleash;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.w3c.dom.Document;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;

@Singleton
// TODO add serialization of metadata as a reporting feature!
public class ReleaseMetadata {
  @Inject
  private MavenProject project;
  @Inject
  @Named("tagNamePattern")
  private String tagNamePattern;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  private String initialScmRevision;
  private String scmRevisionBeforeNextDevVersion;
  private String scmRevisionAfterNextDevVersion;
  private String scmRevisionBeforeTag;
  private String scmRevisionAfterTag;
  private Map<ReleasePhase, Set<ArtifactCoordinates>> artifactCoordinates;
  private String scmTagName;
  private RemoteRepository deploymentRepository;
  private Map<ArtifactCoordinates, Document> cachedPomDocs;
  private Set<Artifact> releaseArtifacts;
  private Map<String, Scm> oldScmSettings;

  private ReleaseMetadata() {
    int numPhases = ReleasePhase.values().length;
    this.artifactCoordinates = Maps.newHashMapWithExpectedSize(numPhases);
    for (ReleasePhase phase : ReleasePhase.values()) {
      this.artifactCoordinates.put(phase, Sets.<ArtifactCoordinates> newHashSet());
    }
  }

  @PostConstruct
  public void init() {
    // setting the artifact version to a release version temporarily since the dist repository is checks for a snapshot
    // version of the artifact. Maybe this can be implemented in a different manner but then we would have to setup the
    // repository manually
    org.apache.maven.artifact.Artifact projectArtifact = this.project.getArtifact();
    String oldVersion = projectArtifact.getVersion();
    projectArtifact.setVersion("1");

    // getting the remote repo
    this.deploymentRepository = RepositoryUtils.toRepo(this.project.getDistributionManagementArtifactRepository());

    // resetting the artifact version
    projectArtifact.setVersion(oldVersion);

    // caching of the parsed pom documents for later reversal in case of failure
    this.cachedPomDocs = Maps.newHashMapWithExpectedSize(this.reactorProjects.size());
    for (MavenProject p : this.reactorProjects) {
      // It is necessary to use the empty project version here and during document retrieval since the versions change
      // during build which would otherwise result in a cache miss.
      this.cachedPomDocs.put(new ArtifactCoordinates(p.getGroupId(), p.getArtifactId(),
          MavenProject.EMPTY_PROJECT_VERSION, p.getPackaging()), PomUtil.parsePOM(p));
    }
  }

  public void setInitialScmRevision(String scmRevision) {
    this.initialScmRevision = scmRevision;
  }

  public String getInitialScmRevision() {
    return this.initialScmRevision;
  }

  public void setScmRevisionBeforeNextDevVersion(String scmRevisionBeforeNextDevVersion) {
    this.scmRevisionBeforeNextDevVersion = scmRevisionBeforeNextDevVersion;
  }

  public String getScmRevisionBeforeNextDevVersion() {
    return this.scmRevisionBeforeNextDevVersion;
  }

  public void setScmRevisionAfterNextDevVersion(String scmRevisionAfterNextDevVersion) {
    this.scmRevisionAfterNextDevVersion = scmRevisionAfterNextDevVersion;
  }

  public String getScmRevisionAfterNextDevVersion() {
    return this.scmRevisionAfterNextDevVersion;
  }

  public void setScmRevisionBeforeTag(String scmRevisionBeforeTag) {
    this.scmRevisionBeforeTag = scmRevisionBeforeTag;
  }

  public String getScmRevisionBeforeTag() {
    return this.scmRevisionBeforeTag;
  }

  public void setScmRevisionAfterTag(String scmRevisionAfterTag) {
    this.scmRevisionAfterTag = scmRevisionAfterTag;
  }

  public String getScmRevisionAfterTag() {
    return this.scmRevisionAfterTag;
  }

  public void addArtifactCoordinates(ArtifactCoordinates coordinates, ReleasePhase phase) {
    this.artifactCoordinates.get(phase).add(coordinates);
  }

  public Map<ReleasePhase, ArtifactCoordinates> getArtifactCoordinatesByPhase(String groupId, String artifactId) {
    Map<ReleasePhase, ArtifactCoordinates> result = Maps.newHashMapWithExpectedSize(this.artifactCoordinates.size());
    for (ReleasePhase phase : this.artifactCoordinates.keySet()) {
      for (ArtifactCoordinates coordinates : this.artifactCoordinates.get(phase)) {
        if (Objects.equal(coordinates.getArtifactId(), artifactId)
            && Objects.equal(coordinates.getGroupId(), groupId)) {
          result.put(phase, coordinates);
          break;
        }
      }
    }
    return result;
  }

  public String getScmTagName() {
    if (this.scmTagName == null) {
      this.scmTagName = ReleaseUtil.getTagName(this.tagNamePattern, this.project);
    }
    return this.scmTagName;
  }

  public RemoteRepository getDeploymentRepository() {
    return this.deploymentRepository;
  }

  public Document getCachedDocument(MavenProject p) {
    ArtifactCoordinates coordinates = new ArtifactCoordinates(p.getGroupId(), p.getArtifactId(),
        MavenProject.EMPTY_PROJECT_VERSION, p.getPackaging());
    return this.cachedPomDocs.get(coordinates);
  }

  public void addReleaseArtifact(Artifact artifact) {
    if (this.releaseArtifacts == null) {
      this.releaseArtifacts = Sets.newHashSet();
    }
    this.releaseArtifacts.add(artifact);
  }

  public Set<Artifact> getReleaseArtifacts() {
    return this.releaseArtifacts;
  }

  public void cacheScmSettings(String simpleCoordinates, Scm scm) {
    if (this.oldScmSettings == null) {
      this.oldScmSettings = Maps.newHashMap();
    }
    this.oldScmSettings.put(simpleCoordinates, scm);
  }

  public Scm getCachedScmSettings(String simpleProjectCoordinates) {
    return this.oldScmSettings.get(simpleProjectCoordinates);
  }
}
