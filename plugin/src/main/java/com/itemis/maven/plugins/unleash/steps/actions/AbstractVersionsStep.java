package com.itemis.maven.plugins.unleash.steps.actions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.PomHelper;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An abstract step for version upgrades in POMs.
 *
 * @author <a href="mailto:tangtheone@gmail.com">Pei-Tang Huang</a>
 * @since 2.6.1
 */
public abstract class AbstractVersionsStep implements CDIMojoProcessingStep {
  @Inject
  protected Logger log;

  @Inject
  protected ReleaseMetadata metadata;

  @Inject
  @Named("reactorProjects")
  protected List<MavenProject> reactorProjects;

  protected Map<ArtifactCoordinates, Document> cachedPOMs;

  @Inject
  @Named("updateReactorDependencyVersion")
  protected boolean updateReactorDependencyVersion;

  // Raw models is only used for dependency version updating currently.
  // This may cause inconsistencies between reactorProjects and updated POMs.
  // However, I have no confidence in spread it out of this step.
  // TODO make the whole plugin work on raw models? (after having integration tests...)
  private LoadingCache<MavenProject, Model> rawModels = CacheBuilder.newBuilder().build(
          new CacheLoader<MavenProject, Model>() {
            @Override
            public Model load(MavenProject project) throws Exception {
              return PomHelper.getRawModel(project);
            }
          });

  protected Document loadAndProcess(MavenProject project) {
    Document document = PomUtil.parsePOM(project);
    setProjectVersion(project, document);
    setParentVersion(project, document);
    if (updateReactorDependencyVersion) {
      setProjectReactorDependenciesVersion(project, document);
      setProjectReactorDependencyManagementVersion(project, document);
      setProfilesReactorDependenciesVersion(project, document);
      setProfilesReactorDependencyManagementVersion(project, document);
    }
    return document;
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
            .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVersion = coordinatesByPhase.get(previousReleasePhase()).getVersion();
    String newVersion = coordinatesByPhase.get(currentReleasePhase()).getVersion();
    logProjectVersionUpdate(project, oldVersion, newVersion);
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
  }

  protected void logProjectVersionUpdate(MavenProject project, String oldVersion, String newVersion) {
    if (log.isDebugEnabled()) {
      log.debug("\tUpdate of module version '" + project.getGroupId() + ":"
              + project.getArtifact() + "' [" + oldVersion + " => " + newVersion + "]");
    }
  }

  private void setParentVersion(MavenProject project, Document document) {
    Parent parent = project.getModel().getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
              .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(previousReleasePhase());
      ArtifactCoordinates newCoordinates = coordinatesByPhase.get(currentReleasePhase());

      // null indicates that the parent is not part of the reactor projects since no release version had been calculated
      // for it
      if (newCoordinates != null) {
        logParentVersionUpdate(project, oldCoordinates, newCoordinates);
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
      }
    }
  }

  protected void logParentVersionUpdate(MavenProject project, ArtifactCoordinates oldCoordinates, ArtifactCoordinates newCoordinates) {
    if (log.isDebugEnabled()) {
      log.debug("\tUpdate of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
              + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
    }
  }

  private void setProjectReactorDependenciesVersion(MavenProject project, Document document) {
    final String dependenciesPath = "/";
    List<Dependency> dependencies = rawModels.getUnchecked(project).getDependencies();
    for (Dependency dependency : dependencies) {
      trySetDependencyVersionFromReactorProjects(project, document, dependenciesPath, dependency);
    }
  }

  private void setProjectReactorDependencyManagementVersion(MavenProject project, Document document) {
    DependencyManagement dependencyManagement = rawModels.getUnchecked(project).getDependencyManagement();
    if (dependencyManagement != null) {
      String dependenciesPath = "/dependencyManagement";
      List<Dependency> dependencies = dependencyManagement.getDependencies();
      for (Dependency dependency : dependencies) {
        trySetDependencyVersionFromReactorProjects(project, document, dependenciesPath, dependency);
      }
    }
  }

  private void setProfilesReactorDependenciesVersion(MavenProject project, Document document) {
    List<Profile> profiles = rawModels.getUnchecked(project).getProfiles();
    for (Profile profile : profiles) {
      final String dependenciesPath = "/profiles/profile[id[text()='" + profile.getId() + "']]";
      List<Dependency> dependencies = profile.getDependencies();
      for (Dependency dependency : dependencies) {
        trySetDependencyVersionFromReactorProjects(project, document, dependenciesPath, dependency);
      }
    }
  }

  private void setProfilesReactorDependencyManagementVersion(MavenProject project, Document document) {
    List<Profile> profiles = rawModels.getUnchecked(project).getProfiles();
    for (Profile profile : profiles) {
      final String dependenciesPath = "/profiles/profile[id[text()='" + profile.getId() + "']]/dependencyManagement";
      DependencyManagement dependencyManagement = profile.getDependencyManagement();
      if (dependencyManagement != null) {
        List<Dependency> dependencies = dependencyManagement.getDependencies();
        for (Dependency dependency : dependencies) {
          trySetDependencyVersionFromReactorProjects(project, document, dependenciesPath, dependency);
        }
      }
    }
  }

  protected abstract ReleasePhase previousReleasePhase();

  protected abstract ReleasePhase currentReleasePhase();

  private void trySetDependencyVersionFromReactorProjects(MavenProject project, Document document, String dependenciesPath, Dependency dependency) {
    for (MavenProject reactorProject : reactorProjects) {
      if (isReactorDependency(reactorProject, dependency)) {
        Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
                .getArtifactCoordinatesByPhase(dependency.getGroupId(), dependency.getArtifactId());
        ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(previousReleasePhase());
        ArtifactCoordinates newCoordinates = coordinatesByPhase.get(currentReleasePhase());

        if (newCoordinates == null || oldCoordinates == null) {
          // the dependency is not part of the reactor projects since no release version had been calculated for it

        } else if (dependency.getVersion() == null) {
          // version was managed somewhere

        } else if (Objects.equals(dependency.getVersion(), oldCoordinates.getVersion())) {
          this.log.debug("\tUpdate of dependency '" + dependency.getGroupId() + ":"
                  + dependency.getArtifactId() + "' version in '" + dependenciesPath + "' of module '"
                  + project.getGroupId() + ":" + project.getArtifact() + "' [" + oldCoordinates.getVersion() + " => "
                  + newCoordinates.getVersion() + "]");
          PomUtil.setDependencyVersion(dependency, document, dependenciesPath, newCoordinates.getVersion());
        }
      }
    }
  }

  private boolean isReactorDependency(MavenProject project, Dependency dependency) {
    String groupId = dependency.getGroupId();
    String artifactId = dependency.getArtifactId();

    Model model = rawModels.getUnchecked(project);
    String reactorGroupId = model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId();
    String reactorArtifactId = model.getArtifactId();

    return Objects.equals(groupId, reactorGroupId) && Objects.equals(artifactId, reactorArtifactId);
  }
}
