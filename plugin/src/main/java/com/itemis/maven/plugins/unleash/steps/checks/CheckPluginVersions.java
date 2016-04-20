package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;
import com.itemis.maven.plugins.unleash.util.functions.PluginToString;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotPlugin;

@ProcessingStep(@Goal(name = "perform", stepNumber = 13))
public class CheckPluginVersions implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private PluginDescriptor pluginDescriptor;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.debug("Checking that none of the reactor projects contain SNAPSHOT plugins.");

    Multimap<MavenProject, String> snapshotsByProject = HashMultimap.create();

    for (MavenProject project : this.reactorProjects) {
      snapshotsByProject.putAll(project, getSnapshotsFromManagement(project));
      snapshotsByProject.putAll(project, getSnapshots(project));
      snapshotsByProject.putAll(project, getSnapshotsFromAllProfiles(project));

      removePluginForIntegrationTests(snapshotsByProject);
    }

    if (!snapshotsByProject.values().isEmpty()) {
      this.log.debug(
          "The following list contains all SNAPSHOT plugins grouped by the reactor project where they are referenced:");
      for (MavenProject project : snapshotsByProject.keySet()) {
        Collection<String> snapshots = snapshotsByProject.get(project);
        if (!snapshots.isEmpty()) {
          this.log.debug("\t[PROJECT] " + ProjectToString.INSTANCE.apply(project));
          for (String plugin : snapshots) {
            this.log.debug("\t\t[PLUGIN] " + plugin);
          }
        }
      }
      throw new MojoFailureException("The project cannot be released due to one or more SNAPSHOT plugins!");
    }
  }

  private Set<String> getSnapshotsFromManagement(MavenProject project) {
    Build build = project.getBuild();
    if (build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if (pluginManagement != null) {
        Collection<Plugin> snapshots = Collections2.filter(pluginManagement.getPlugins(), IsSnapshotPlugin.INSTANCE);
        return Sets.newHashSet(Collections2.transform(snapshots, PluginToString.INSTANCE));
      }
    }
    return Collections.emptySet();
  }

  private Set<String> getSnapshots(MavenProject project) {
    Build build = project.getBuild();
    if (build != null) {
      Collection<Plugin> snapshots = Collections2.filter(build.getPlugins(), IsSnapshotPlugin.INSTANCE);
      return Sets.newHashSet(Collections2.transform(snapshots, PluginToString.INSTANCE));
    }
    return Collections.emptySet();
  }

  private Set<String> getSnapshotsFromAllProfiles(MavenProject project) {
    Set<String> snapshots = Sets.newHashSet();
    List<Profile> profiles = project.getModel().getProfiles();
    if (profiles != null) {
      for (Profile profile : profiles) {
        snapshots.addAll(getSnapshotsFromManagement(profile));
        snapshots.addAll(getSnapshots(profile));
      }
    }
    return snapshots;
  }

  private Set<String> getSnapshotsFromManagement(Profile profile) {
    BuildBase build = profile.getBuild();
    if (build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if (pluginManagement != null) {
        Collection<Plugin> snapshots = Collections2.filter(pluginManagement.getPlugins(), IsSnapshotPlugin.INSTANCE);
        return Sets.newHashSet(Collections2.transform(snapshots, PluginToString.INSTANCE));
      }
    }
    return Collections.emptySet();
  }

  private Set<String> getSnapshots(Profile profile) {
    BuildBase build = profile.getBuild();
    if (build != null) {
      Collection<Plugin> snapshots = Collections2.filter(build.getPlugins(), IsSnapshotPlugin.INSTANCE);
      return Sets.newHashSet(Collections2.transform(snapshots, PluginToString.INSTANCE));
    }
    return Collections.emptySet();
  }

  private void removePluginForIntegrationTests(Multimap<MavenProject, String> snapshotsByProject) {
    if (ReleaseUtil.isIntegrationtest()) {
      for (Iterator<Entry<MavenProject, String>> i = snapshotsByProject.entries().iterator(); i.hasNext();) {
        Entry<MavenProject, String> entry = i.next();
        if (Objects.equals(entry.getValue(), PluginToString.INSTANCE.apply(this.pluginDescriptor.getPlugin()))) {
          i.remove();
        }
      }
    }
  }
}
