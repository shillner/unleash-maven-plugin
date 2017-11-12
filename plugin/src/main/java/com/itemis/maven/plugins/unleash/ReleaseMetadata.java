package com.itemis.maven.plugins.unleash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
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
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;

/**
 * Provides global metadata used during the release process. These metadata evolve during the release process.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Singleton
public class ReleaseMetadata {
  private static final String PROPERTIES_KEY_REL_ARTIFACT = "release.artifact.";
  private static final String PROPERTIES_KEY_REL_REPO_URL = "release.deploymentRepository.url";
  private static final String PROPERTIES_KEY_REL_REPO_ID = "release.deploymentRepository.id";
  private static final String PROPERTIES_KEY_SCM_REV_AFTER_DEV = "scm.rev.afterNextDev";
  private static final String PROPERTIES_KEY_SCM_REV_BEFORE_DEV = "scm.rev.beforeNextDev";
  private static final String PROPERTIES_KEY_SCM_REV_AFTER_TAG = "scm.rev.afterTag";
  private static final String PROPERTIES_KEY_SCM_REV_BEFORE_TAG = "scm.rev.beforeTag";
  private static final String PROPERTIES_KEY_SCM_REV_INITIAL = "scm.rev.initial";
  private static final String PROPERTIES_KEY_TAG_PATTERN = "scm.tag.namePattern";
  private static final String PROPERTIES_KEY_TAG_NAME = "scm.tag.name";
  private static final String PROPERTIES_KEY_VERSION_REACTOR_PRE_RELEASE = "version.reactor.pre.release";
  private static final String PROPERTIES_KEY_VERSION_REACTOR_RELEASE = "version.reactor.release";
  private static final String PROPERTIES_KEY_VERSION_REACTOR_POST_RELEASE = "version.reactor.post.release";

  @Inject
  private MavenProject project;
  @Inject
  private PluginParameterExpressionEvaluator expressionEvaluator;
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
  private Set<Artifact> releaseArtifacts;
  private Map<ArtifactCoordinates, Scm> cachedScmSettings;
  private Map<ArtifactCoordinates, Document> originalPOMs;

  private ReleaseMetadata() {
    int numPhases = ReleasePhase.values().length;
    this.artifactCoordinates = Maps.newHashMapWithExpectedSize(numPhases);
    for (ReleasePhase phase : ReleasePhase.values()) {
      this.artifactCoordinates.put(phase, Sets.<ArtifactCoordinates> newHashSet());
    }
    this.cachedScmSettings = Maps.newHashMap();
    this.originalPOMs = new HashMap<>();
  }

  @PostConstruct
  public void init() {
    // setting the artifact version to a release version temporarily since the dist repository checks for a snapshot
    // version of the artifact. Maybe this can be implemented in a different manner but then we would have to setup the
    // repository manually
    org.apache.maven.artifact.Artifact projectArtifact = this.project.getArtifact();
    String oldVersion = projectArtifact.getVersion();
    projectArtifact.setVersion("1");

    // getting the remote repo
    this.deploymentRepository = RepositoryUtils.toRepo(this.project.getDistributionManagementArtifactRepository());

    // resetting the artifact version
    projectArtifact.setVersion(oldVersion);

    for (MavenProject p : this.reactorProjects) {
      // puts the initial module artifact coordinates into the cache
      addArtifactCoordinates(ProjectToCoordinates.POM.apply(p), ReleasePhase.PRE_RELEASE);

      // caching of SCM settings of every POM in order to go back to it before setting next dev version
      this.cachedScmSettings.put(ProjectToCoordinates.EMPTY_VERSION.apply(p), p.getModel().getScm());

      this.originalPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(p), PomUtil.parsePOM(p));
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
      this.scmTagName = ReleaseUtil.getTagName(this.tagNamePattern, this.project, this.expressionEvaluator);
    }
    return this.scmTagName;
  }

  public RemoteRepository getDeploymentRepository() {
    return this.deploymentRepository;
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

  public Scm getCachedScmSettings(MavenProject p) {
    return this.cachedScmSettings.get(ProjectToCoordinates.EMPTY_VERSION.apply(p));
  }

  public Document getCachedOriginalPOM(MavenProject p) {
    return this.originalPOMs.get(ProjectToCoordinates.EMPTY_VERSION.apply(p));
  }

  public Properties toProperties() {
    Properties p = new Properties();
    addVersionInfo(p);
    addScmTagInfo(p);
    addScmRevisions(p);
    addDeploymentRepositoryInfo(p);
    addReleaseArtifacts(p);
    return p;
  }

  private void addVersionInfo(Properties p) {
    Map<ReleasePhase, ArtifactCoordinates> reactorCoordinates = getArtifactCoordinatesByPhase(this.project.getGroupId(),
        this.project.getArtifactId());

    if (reactorCoordinates != null) {
      ArtifactCoordinates preReleaseCoordinates = reactorCoordinates.get(ReleasePhase.PRE_RELEASE);
      ArtifactCoordinates releaseCoordinates = reactorCoordinates.get(ReleasePhase.RELEASE);
      ArtifactCoordinates postReleaseCoordinates = reactorCoordinates.get(ReleasePhase.POST_RELEASE);

      if (preReleaseCoordinates != null) {
        p.setProperty(PROPERTIES_KEY_VERSION_REACTOR_PRE_RELEASE, preReleaseCoordinates.getVersion());
      }
      if (releaseCoordinates != null) {
        p.setProperty(PROPERTIES_KEY_VERSION_REACTOR_RELEASE, releaseCoordinates.getVersion());
      }
      if (postReleaseCoordinates != null) {
        p.setProperty(PROPERTIES_KEY_VERSION_REACTOR_POST_RELEASE, postReleaseCoordinates.getVersion());
      }
    }
  }

  private void addScmTagInfo(Properties p) {
    p.setProperty(PROPERTIES_KEY_TAG_PATTERN, this.tagNamePattern);
    p.setProperty(PROPERTIES_KEY_TAG_NAME, this.scmTagName != null ? this.scmTagName : StringUtils.EMPTY);
  }

  private void addScmRevisions(Properties p) {
    p.setProperty(PROPERTIES_KEY_SCM_REV_INITIAL,
        this.initialScmRevision != null ? this.initialScmRevision : StringUtils.EMPTY);
    p.setProperty(PROPERTIES_KEY_SCM_REV_BEFORE_TAG,
        this.scmRevisionBeforeTag != null ? this.scmRevisionBeforeTag : StringUtils.EMPTY);
    p.setProperty(PROPERTIES_KEY_SCM_REV_AFTER_TAG,
        this.scmRevisionAfterTag != null ? this.scmRevisionAfterTag : StringUtils.EMPTY);
    p.setProperty(PROPERTIES_KEY_SCM_REV_BEFORE_DEV,
        this.scmRevisionBeforeNextDevVersion != null ? this.scmRevisionBeforeNextDevVersion : StringUtils.EMPTY);
    p.setProperty(PROPERTIES_KEY_SCM_REV_AFTER_DEV,
        this.scmRevisionAfterNextDevVersion != null ? this.scmRevisionAfterNextDevVersion : StringUtils.EMPTY);
  }

  private void addReleaseArtifacts(Properties p) {
    if (this.releaseArtifacts == null) {
      return;
    }

    int index = 0;
    for (Artifact a : this.releaseArtifacts) {
      p.setProperty(PROPERTIES_KEY_REL_ARTIFACT + index, a.toString());
      index++;
    }
  }

  private void addDeploymentRepositoryInfo(Properties p) {
    p.setProperty(PROPERTIES_KEY_REL_REPO_ID,
        this.deploymentRepository != null ? this.deploymentRepository.getId() : "");
    p.setProperty(PROPERTIES_KEY_REL_REPO_URL,
        this.deploymentRepository != null ? this.deploymentRepository.getUrl() : "");
  }
}
