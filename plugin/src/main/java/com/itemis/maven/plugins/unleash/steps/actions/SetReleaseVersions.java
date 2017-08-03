package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.PomHelper;
import org.w3c.dom.Document;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Updates the POMs of all project modules with the previously calculated release versions. This step updates project
 * versions as well as parent versions.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "setReleaseVersions", description = "Updates the POMs of all project modules with their release versions calculated previously.", requiresOnline = false)
public class SetReleaseVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  private Map<ArtifactCoordinates, Document> cachedPOMs;
  @Inject
  @Named("updateReactorDependencyVersion")
  private boolean updateReactorDependencyVersion;

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

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Updating project modules with release versions");
    this.cachedPOMs = Maps.newHashMap();

    for (MavenProject project : this.reactorProjects) {
      this.cachedPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(project), PomUtil.parsePOM(project));

      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document);
        setParentVersion(project, document);
        if (updateReactorDependencyVersion) {
          setProjectReactorDependenciesVersion(project, document);
          setProjectReactorDependencyManagementVersion(project, document);
          setProfilesReactorDependenciesVersion(project, document);
          setProfilesReactorDependencyManagementVersion(project, document);
        }
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for release.", t);
      }
    }
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVersion = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE).getVersion();
    String newVersion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();
    this.log.debug("\tUpdate of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' ["
        + oldVersion + " => " + newVersion + "]");
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
  }

  private void setParentVersion(MavenProject project, Document document) {
    Parent parent = project.getModel().getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE);
      ArtifactCoordinates newCoordinates = coordinatesByPhase.get(ReleasePhase.RELEASE);

      // null indicates that the parent is not part of the reactor projects since no release version had been calculated
      // for it
      if (newCoordinates != null) {
        this.log.debug("\tUpdate of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
      }
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

  private void trySetDependencyVersionFromReactorProjects(MavenProject project, Document document, String dependenciesPath, Dependency dependency) {
    for (MavenProject reactorProject : reactorProjects) {
      if (isReactorDependency(reactorProject, dependency)) {
        Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
                .getArtifactCoordinatesByPhase(dependency.getGroupId(), dependency.getArtifactId());
        ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE);
        ArtifactCoordinates newCoordinates = coordinatesByPhase.get(ReleasePhase.RELEASE);

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

  @RollbackOnError
  public void rollback() throws MojoExecutionException {
    this.log.info("Rollback of release version updating for all project modules");

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tRolling back modifications on POM of module '" + ProjectToString.INSTANCE.apply(project) + "'");

      Document document = this.cachedPOMs.get(ProjectToCoordinates.EMPTY_VERSION.apply(project));
      if (document != null) {
        try {
          PomUtil.writePOM(document, project);
        } catch (Throwable t) {
          throw new MojoExecutionException(
              "Could not revert the setting of release versions after a failed release build.", t);
        }
      }
    }
  }
}
