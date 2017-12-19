package com.itemis.maven.plugins.unleash.steps.actions.tycho;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.VersionsEngine;
import org.eclipse.tycho.versions.pom.PomFile;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * An abstract step for version upgrades using Eclipse Tycho which upgrades versions in POMs as well as MANIFESTs and
 * bundle references.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.1.0
 */
public abstract class AbstractTychoVersionsStep implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  private MavenProject project;
  @Inject
  private PlexusContainer plexus;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  private Map<ArtifactCoordinates, Document> cachedPOMs;
  private Map<ArtifactCoordinates, String> cachedModuleVersions;
  private ProjectMetadataReader metadataReader;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.cachedPOMs = Maps.newHashMap();
    this.cachedModuleVersions = Maps.newHashMap();

    VersionsEngine versionsEngine = initializeVersionsEngine();
    try {
      for (MavenProject module : this.reactorProjects) {
        ArtifactCoordinates coordinates = ProjectToCoordinates.EMPTY_VERSION.apply(module);
        Optional<Document> parsedPOM = PomUtil.parsePOM(this.project);
        if (parsedPOM.isPresent()) {
          this.cachedPOMs.put(coordinates, parsedPOM.get());
        }
        this.cachedModuleVersions.put(coordinates, module.getVersion());

        Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
            .getArtifactCoordinatesByPhase(module.getGroupId(), module.getArtifactId());
        String version = coordinatesByPhase.get(currentReleasePhase()).getVersion();

        versionsEngine.addVersionChange(module.getArtifactId(), version);

        if (module.getModel().getVersion() != null) {
          module.getModel().setVersion(version);
        }
      }

      versionsEngine.apply();
      adaptProjectMetadataWithNewVersions();
    } catch (IOException e) {
      throw new MojoExecutionException("Error during tycho version upgrade.", e);
    }
  }

  private void adaptProjectMetadataWithNewVersions() {
    // this needs to be done in order to make the changes reversible
    // if the metadata is not adapted reverting the changes wouldn't be possible for pom-less builds since the metadata
    // is initialized only once before the build starts
    this.metadataReader.getProjects().forEach(data -> {
      PomFile pom = data.getMetadata(PomFile.class);

      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(pom.getGroupId(), pom.getArtifactId());
      String version = coordinatesByPhase.get(currentReleasePhase()).getVersion();

      pom.setVersion(version);
    });
  }

  protected abstract ReleasePhase currentReleasePhase();

  private <T> T lookup(Class<T> clazz) throws MojoFailureException {
    try {
      return this.plexus.lookup(clazz);
    } catch (ComponentLookupException e) {
      throw new MojoFailureException("Could not lookup required component", e);
    }
  }

  @RollbackOnError
  public void rollback() throws MojoExecutionException, MojoFailureException {
    VersionsEngine versionsEngine = initializeVersionsEngine();
    try {
      // first add all module version changes to the versions engine of tycho and execute the change command
      for (MavenProject module : this.reactorProjects) {
        String version = this.cachedModuleVersions.get(ProjectToCoordinates.EMPTY_VERSION.apply(module));
        versionsEngine.addVersionChange(module.getArtifactId(), version);
      }
      versionsEngine.apply();

      // second step is to revert all pom changes by simply replacing the poms
      for (MavenProject module : this.reactorProjects) {
        this.log.debug(
            "\tRolling back modifications on POM of module '" + ProjectToString.INSTANCE.apply(this.project) + "'");
        ArtifactCoordinates coordinates = ProjectToCoordinates.EMPTY_VERSION.apply(module);

        Document document = this.cachedPOMs.get(coordinates);
        if (document != null) {
          try {
            PomUtil.writePOM(document, this.project);
          } catch (Throwable t) {
            throw new MojoExecutionException("Could not revert the version update after a failed release build.", t);
          }
        }

        String version = this.cachedModuleVersions.get(coordinates);
        if (module.getModel().getVersion() != null) {
          module.getModel().setVersion(version);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not revert the version update after a failed release build.", e);
    }
  }

  private VersionsEngine initializeVersionsEngine() throws MojoExecutionException, MojoFailureException {
    if (this.metadataReader == null) {
      this.metadataReader = lookup(ProjectMetadataReader.class);
      try {
        this.metadataReader.addBasedir(this.project.getBasedir());
      } catch (IOException e) {
        throw new MojoExecutionException("Tycho was unable to read the project structure!", e);
      }
    }

    VersionsEngine versionsEngine = lookup(VersionsEngine.class);
    versionsEngine.setUpdateVersionRangeMatchingBounds(false);
    versionsEngine.setProjects(this.metadataReader.getProjects());

    return versionsEngine;
  }
}
