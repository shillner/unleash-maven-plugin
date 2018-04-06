package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;
import com.itemis.maven.plugins.unleash.util.functions.DependencyToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.PluginToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotDependency;

/**
 * Checks that none of the project modules contains plugins that have SNAPSHOT dependencies since this would potentially
 * lead to
 * non-reproducible release artifacts.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "checkPluginDependencies", description = "Checks that the plugins used by the projects do not reference SNAPSHOT dependencies to avoid unreproducible release aritfacts.", requiresOnline = false)
public class CheckPluginDependencyVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private PluginDescriptor pluginDescriptor;
  @Inject
  @Named("profiles")
  private List<String> profiles;
  @Inject
  @Named("releaseArgs")
  private Properties releaseArgs;
  @Inject
  private Settings settings;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Checking that none of the reactor project's plugins contain SNAPSHOT dependencies.");

    Map<MavenProject, PomPropertyResolver> propertyResolvers = Maps
        .newHashMapWithExpectedSize(this.reactorProjects.size());
    Map<MavenProject, Multimap<ArtifactCoordinates, ArtifactCoordinates>> snapshotsByProjectAndPlugin = Maps
        .newHashMapWithExpectedSize(this.reactorProjects.size());
    boolean hasSnapshots = false;
    for (MavenProject project : this.reactorProjects) {
      this.log.debug(
          "\tChecking plugin dependencies of reactor project '" + ProjectToString.INSTANCE.apply(project) + "':");
      PomPropertyResolver propertyResolver = new PomPropertyResolver(project, this.settings, this.profiles,
          this.releaseArgs);
      propertyResolvers.put(project, propertyResolver);

      Multimap<ArtifactCoordinates, ArtifactCoordinates> snapshots = HashMultimap.create();
      snapshots.putAll(getSnapshotsFromManagement(project, propertyResolver));
      snapshots.putAll(getSnapshots(project, propertyResolver));
      snapshots.putAll(getSnapshotsFromAllProfiles(project, propertyResolver));

      removePluginForIntegrationTests(snapshots);

      snapshotsByProjectAndPlugin.put(project, snapshots);
      if (!snapshots.isEmpty()) {
        hasSnapshots = true;
      }
    }

    failIfSnapshotsAreReferenced(hasSnapshots, snapshotsByProjectAndPlugin, propertyResolvers);
  }

  private void failIfSnapshotsAreReferenced(boolean hasSnapshots,
      Map<MavenProject, Multimap<ArtifactCoordinates, ArtifactCoordinates>> snapshotsByProjectAndPlugin,
      Map<MavenProject, PomPropertyResolver> propertyResolvers) throws MojoFailureException {
    if (hasSnapshots) {
      this.log.error(
          "\tThere are plugins with SNAPSHOT dependencies! The following list contains all SNAPSHOT dependencies grouped by plugin and module:");

      for (MavenProject p : snapshotsByProjectAndPlugin.keySet()) {
        PomPropertyResolver propertyResolver = propertyResolvers.get(p);
        Multimap<ArtifactCoordinates, ArtifactCoordinates> snapshots = snapshotsByProjectAndPlugin.get(p);

        if (!snapshots.isEmpty()) {
          this.log.error("\t\t[PROJECT] " + ProjectToString.INSTANCE.apply(p));

          for (ArtifactCoordinates plugin : snapshots.keySet()) {
            this.log.error("\t\t\t[PLUGIN] " + plugin);

            for (ArtifactCoordinates dependency : snapshots.get(plugin)) {
              String resolvedVersion = propertyResolver.expandPropertyReferences(dependency.getVersion());
              String coordinates = dependency.toString();
              if (!Objects.equal(resolvedVersion, dependency.getVersion())) {
                coordinates = coordinates + " (resolves to " + resolvedVersion + ")";
              }
              this.log.error("\t\t\t\t[DEPENDENCY] " + coordinates);
            }
          }
        }
      }
      throw new MojoFailureException("The project cannot be released due to one or more SNAPSHOT plugin-dependencies!");
    }
  }

  private Multimap<ArtifactCoordinates, ArtifactCoordinates> getSnapshotsFromManagement(MavenProject project,
      PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking managed plugins");
    Multimap<ArtifactCoordinates, ArtifactCoordinates> result = HashMultimap.create();
    Build build = project.getBuild();
    if (build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if (pluginManagement != null) {
        for (Plugin plugin : pluginManagement.getPlugins()) {
          Collection<Dependency> snapshots = Collections2.filter(plugin.getDependencies(),
              new IsSnapshotDependency(propertyResolver));
          if (!snapshots.isEmpty()) {
            result.putAll(PluginToCoordinates.INSTANCE.apply(plugin),
                Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
          }
        }
      }
    }
    return result;
  }

  private Multimap<ArtifactCoordinates, ArtifactCoordinates> getSnapshots(MavenProject project,
      PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking direct plugin references");
    Multimap<ArtifactCoordinates, ArtifactCoordinates> result = HashMultimap.create();
    Build build = project.getBuild();
    if (build != null) {
      for (Plugin plugin : build.getPlugins()) {
        Collection<Dependency> snapshots = Collections2.filter(plugin.getDependencies(),
            new IsSnapshotDependency(propertyResolver));
        if (!snapshots.isEmpty()) {
          result.putAll(PluginToCoordinates.INSTANCE.apply(plugin),
              Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
        }
      }
    }
    return result;
  }

  // IDEA implement to use active profiles only (maybe create the effective pom using api with the release profiles)
  private Multimap<ArtifactCoordinates, ArtifactCoordinates> getSnapshotsFromAllProfiles(MavenProject project,
      PomPropertyResolver propertyResolver) {
    Multimap<ArtifactCoordinates, ArtifactCoordinates> result = HashMultimap.create();
    List<Profile> profiles = project.getModel().getProfiles();
    if (profiles != null) {
      for (Profile profile : profiles) {
        result.putAll(getSnapshotsFromManagement(profile, propertyResolver));
        result.putAll(getSnapshots(profile, propertyResolver));
      }
    }
    return result;
  }

  private Multimap<ArtifactCoordinates, ArtifactCoordinates> getSnapshotsFromManagement(Profile profile,
      PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking managed plugins of profile '" + profile.getId() + "'");
    Multimap<ArtifactCoordinates, ArtifactCoordinates> result = HashMultimap.create();
    BuildBase build = profile.getBuild();
    if (build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if (pluginManagement != null) {
        for (Plugin plugin : pluginManagement.getPlugins()) {
          Collection<Dependency> snapshots = Collections2.filter(plugin.getDependencies(),
              new IsSnapshotDependency(propertyResolver));
          if (!snapshots.isEmpty()) {
            result.putAll(PluginToCoordinates.INSTANCE.apply(plugin),
                Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
          }
        }
      }
    }
    return result;
  }

  private Multimap<ArtifactCoordinates, ArtifactCoordinates> getSnapshots(Profile profile,
      PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking direct plugin references of profile '" + profile.getId() + "'");
    Multimap<ArtifactCoordinates, ArtifactCoordinates> result = HashMultimap.create();
    BuildBase build = profile.getBuild();
    if (build != null) {
      for (Plugin plugin : build.getPlugins()) {
        Collection<Dependency> snapshots = Collections2.filter(plugin.getDependencies(),
            new IsSnapshotDependency(propertyResolver));
        if (!snapshots.isEmpty()) {
          result.putAll(PluginToCoordinates.INSTANCE.apply(plugin),
              Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
        }
      }
    }
    return result;
  }

  // Removes the unleash plugin itself from the list of violating dependencies if the integration test mode is enabled.
  private void removePluginForIntegrationTests(Multimap<ArtifactCoordinates, ArtifactCoordinates> snapshots) {
    if (ReleaseUtil.isIntegrationtest()) {
      for (Iterator<Entry<ArtifactCoordinates, ArtifactCoordinates>> i = snapshots.entries().iterator(); i.hasNext();) {
        Entry<ArtifactCoordinates, ArtifactCoordinates> entry = i.next();
        if (Objects.equal(entry.getKey(), PluginToCoordinates.INSTANCE.apply(this.pluginDescriptor.getPlugin()))) {
          i.remove();
        }
      }
    }
  }
}
