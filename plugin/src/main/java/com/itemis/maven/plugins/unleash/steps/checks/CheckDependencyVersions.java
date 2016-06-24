package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.functions.DependencyToString;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotDependency;

@ProcessingStep(id = "checkDependencies", description = "Checks that the projects do not reference SNAPSHOT artifacts as dependencies", requiresOnline = false)
public class CheckDependencyVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.debug("Checking that none of the reactor projects contain SNAPSHOT dependencies.");

    Multimap<MavenProject, String> snapshotByProject = HashMultimap.create();

    for (MavenProject project : this.reactorProjects) {
      snapshotByProject.putAll(project, getSnapshotsFromManagement(project));
      snapshotByProject.putAll(project, getSnapshots(project));
      snapshotByProject.putAll(project, getSnapshotsFromAllProfiles(project));
    }

    if (!snapshotByProject.values().isEmpty()) {
      this.log.debug(
          "The following list contains all SNAPSHOT dependencies grouped by the reactor project where they are referenced:");
      for (MavenProject p : snapshotByProject.keySet()) {
        Collection<String> snapshots = snapshotByProject.get(p);
        if (!snapshots.isEmpty()) {
          this.log.debug("\t[PROJECT] " + ProjectToString.INSTANCE.apply(p));
          for (String dependency : snapshots) {
            this.log.debug("\t\t[DEPENDENCY] " + dependency);
          }
        }
      }
      throw new MojoFailureException("The project cannot be released due to one or more SNAPSHOT dependencies!");
    }
  }

  private Set<String> getSnapshotsFromManagement(MavenProject project) {
    DependencyManagement dependencyManagement = project.getDependencyManagement();
    if (dependencyManagement != null) {
      Collection<Dependency> snapshots = Collections2.filter(dependencyManagement.getDependencies(),
          IsSnapshotDependency.INSTANCE);
      HashSet<String> snapshotDependencies = Sets
          .newHashSet(Collections2.transform(snapshots, DependencyToString.INSTANCE));
      filterMultiModuleDependencies(snapshotDependencies);
      return snapshotDependencies;
    }
    return Collections.emptySet();
  }

  private Set<String> getSnapshots(MavenProject project) {
    Collection<Dependency> snapshots = Collections2.filter(project.getDependencies(), IsSnapshotDependency.INSTANCE);
    HashSet<String> snapshotDependencies = Sets
        .newHashSet(Collections2.transform(snapshots, DependencyToString.INSTANCE));
    filterMultiModuleDependencies(snapshotDependencies);
    return snapshotDependencies;
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
    filterMultiModuleDependencies(snapshots);
    return snapshots;
  }

  private Set<String> getSnapshotsFromManagement(Profile profile) {
    DependencyManagement dependencyManagement = profile.getDependencyManagement();
    if (dependencyManagement != null) {
      Collection<Dependency> snapshots = Collections2.filter(dependencyManagement.getDependencies(),
          IsSnapshotDependency.INSTANCE);
      return Sets.newHashSet(Collections2.transform(snapshots, DependencyToString.INSTANCE));
    }
    return Collections.emptySet();
  }

  private Set<String> getSnapshots(Profile profile) {
    Collection<Dependency> snapshots = Collections2.filter(profile.getDependencies(), IsSnapshotDependency.INSTANCE);
    return Sets.newHashSet(Collections2.transform(snapshots, DependencyToString.INSTANCE));
  }

  private void filterMultiModuleDependencies(Set<String> snapshotDependencies) {
    Collection<String> projectCoordinates = Collections2.transform(this.reactorProjects,
        ProjectToString.INCLUDE_PACKAGING);
    for (Iterator<String> i = snapshotDependencies.iterator(); i.hasNext();) {
      String dep = i.next();
      if (projectCoordinates.contains(dep)) {
        i.remove();
      }
    }
  }
}
