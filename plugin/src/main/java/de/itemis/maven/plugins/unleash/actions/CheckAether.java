package de.itemis.maven.plugins.unleash.actions;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import de.itemis.maven.plugins.unleash.util.PomUtil;
import de.itemis.maven.plugins.unleash.util.ReleaseUtil;
import de.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProjectPredicate;

public class CheckAether extends AbstractProcessingAction {
  private String defaultReleaseVersion;
  private boolean allowLocalReleaseArtifacts;

  public CheckAether(String defaultReleaseVersion) {
    this.defaultReleaseVersion = defaultReleaseVersion;
  }

  public void allowLocalReleaseArtifacts(boolean allowLocalReleaseArtifacts) {
    this.allowLocalReleaseArtifacts = allowLocalReleaseArtifacts;
  }

  @Override
  public void execute() {
    List<MavenProject> alreadyReleasedProjects = Lists.newArrayList();

    Collection<MavenProject> snapshotProjects = Collections2.filter(getReactorProjects(),
        new IsSnapshotProjectPredicate());

    for (MavenProject project : snapshotProjects) {
      if (isReleased(project.getGroupId(), project.getArtifactId(),
          ReleaseUtil.getReleaseVersion(project.getVersion(), this.defaultReleaseVersion))) {
        alreadyReleasedProjects.add(project);
      }
    }

    if (!alreadyReleasedProjects.isEmpty()) {
      getLog().error("The following projects have are already present in one of your remote repositories:");
      for (MavenProject p : alreadyReleasedProjects) {
        getLog().error("\t" + PomUtil.getBasicCoordinates(p));
      }
      getLog().error("");
      throw new IllegalStateException(
          "Some of the reactor projects have already been released. Please check your repositories!");
    }
  }

  private boolean isReleased(String groupId, String artifactId, String version) {
    Optional<File> pom = getArtifactResolver().resolve(groupId, artifactId, version,
        Optional.of(PomUtil.ARTIFACT_TYPE_POM), Optional.<String> absent(), this.allowLocalReleaseArtifacts);
    return pom.isPresent();
  }
}
