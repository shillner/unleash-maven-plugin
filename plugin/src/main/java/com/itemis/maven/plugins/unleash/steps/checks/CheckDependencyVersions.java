package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.PomPropertyResolver;
import com.itemis.maven.plugins.unleash.util.functions.DependencyToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotDependency;

/**
 * Checks that none of the project modules has SNAPSHOT dependencies since this would potentially lead to
 * non-reproducible release artifacts.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "checkDependencies", description = "Checks that the project modules do not reference SNAPSHOT dependencies to avoid unreproducible release aritfacts.", requiresOnline = false)
public class CheckDependencyVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Checking that none of the reactor projects contain SNAPSHOT dependencies.");

    Map<MavenProject, PomPropertyResolver> propertyResolvers = Maps
        .newHashMapWithExpectedSize(this.reactorProjects.size());
    Multimap<MavenProject, ArtifactCoordinates> snapshotsByProject = HashMultimap.create();
    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tChecking dependencies of reactor project '" + ProjectToString.INSTANCE.apply(project) + "':");
      PomPropertyResolver propertyResolver = new PomPropertyResolver(project);
      propertyResolvers.put(project, propertyResolver);

      snapshotsByProject.putAll(project, getSnapshotsFromManagement(project, propertyResolver));
      snapshotsByProject.putAll(project, getSnapshots(project, propertyResolver));
      snapshotsByProject.putAll(project, getSnapshotsFromAllProfiles(project, propertyResolver));
    }

    failIfSnapshotsAreReferenced(snapshotsByProject, propertyResolvers);
  }

  private void failIfSnapshotsAreReferenced(Multimap<MavenProject, ArtifactCoordinates> snapshotsByProject,
      Map<MavenProject, PomPropertyResolver> propertyResolvers) throws MojoFailureException {
    if (!snapshotsByProject.values().isEmpty()) {
      this.log.error(
          "\tThere are SNAPSHOT dependency references! The following list contains all SNAPSHOT dependencies grouped by module:");

      for (MavenProject p : snapshotsByProject.keySet()) {
        PomPropertyResolver propertyResolver = propertyResolvers.get(p);
        Collection<ArtifactCoordinates> snapshots = snapshotsByProject.get(p);

        if (!snapshots.isEmpty()) {
          this.log.error("\t\t[PROJECT] " + ProjectToString.INSTANCE.apply(p));

          for (ArtifactCoordinates dependency : snapshots) {
            String resolvedVersion = propertyResolver.expandPropertyReferences(dependency.getVersion());
            String coordinates = dependency.toString();
            if (!Objects.equal(resolvedVersion, dependency.getVersion())) {
              coordinates = coordinates + " (resolves to " + resolvedVersion + ")";
            }
            this.log.error("\t\t\t[DEPENDENCY] " + coordinates);
          }
        }
      }
      throw new MojoFailureException("The project cannot be released due to one or more SNAPSHOT dependencies!");
    }
  }

  private Set<ArtifactCoordinates> getSnapshotsFromManagement(MavenProject project,
      PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking managed dependencies");
    DependencyManagement dependencyManagement = project.getDependencyManagement();
    if (dependencyManagement != null) {
      Collection<Dependency> snapshots = Collections2.filter(dependencyManagement.getDependencies(),
          new IsSnapshotDependency(propertyResolver));
      HashSet<ArtifactCoordinates> snapshotDependencies = Sets
          .newHashSet(Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
      filterMultiModuleDependencies(snapshotDependencies);
      return snapshotDependencies;
    }
    return Collections.emptySet();
  }

  private Set<ArtifactCoordinates> getSnapshots(MavenProject project, PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking direct dependencies");
    Collection<Dependency> snapshots = Collections2.filter(project.getDependencies(),
        new IsSnapshotDependency(propertyResolver));
    HashSet<ArtifactCoordinates> snapshotDependencies = Sets
        .newHashSet(Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
    filterMultiModuleDependencies(snapshotDependencies);
    return snapshotDependencies;
  }

  private Set<ArtifactCoordinates> getSnapshotsFromAllProfiles(MavenProject project,
      PomPropertyResolver propertyResolver) {
    Set<ArtifactCoordinates> snapshots = Sets.newHashSet();
    List<Profile> profiles = project.getModel().getProfiles();
    if (profiles != null) {
      for (Profile profile : profiles) {
        snapshots.addAll(getSnapshotsFromManagement(profile, propertyResolver));
        snapshots.addAll(getSnapshots(profile, propertyResolver));
      }
    }
    filterMultiModuleDependencies(snapshots);
    return snapshots;
  }

  private Set<ArtifactCoordinates> getSnapshotsFromManagement(Profile profile, PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking managed dependencies of profile '" + profile.getId() + "'");
    DependencyManagement dependencyManagement = profile.getDependencyManagement();
    if (dependencyManagement != null) {
      Collection<Dependency> snapshots = Collections2.filter(dependencyManagement.getDependencies(),
          new IsSnapshotDependency(propertyResolver));
      return Sets.newHashSet(Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
    }
    return Collections.emptySet();
  }

  private Set<ArtifactCoordinates> getSnapshots(Profile profile, PomPropertyResolver propertyResolver) {
    this.log.debug("\t\tChecking direct dependencies of profile '" + profile.getId() + "'");
    Collection<Dependency> snapshots = Collections2.filter(profile.getDependencies(),
        new IsSnapshotDependency(propertyResolver));
    return Sets.newHashSet(Collections2.transform(snapshots, DependencyToCoordinates.INSTANCE));
  }

  private void filterMultiModuleDependencies(Set<ArtifactCoordinates> snapshotDependencies) {
    Collection<ArtifactCoordinates> projectCoordinates = Collections2.transform(this.reactorProjects,
        ProjectToCoordinates.INSTANCE);
    for (Iterator<ArtifactCoordinates> i = snapshotDependencies.iterator(); i.hasNext();) {
      ArtifactCoordinates dep = i.next();
      for (ArtifactCoordinates projectCoordinate : projectCoordinates) {
        if (projectCoordinate.equalsGAV(dep)) {
          i.remove();
          break;
        }
      }
    }
  }
}
