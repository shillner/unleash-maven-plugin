package de.itemis.maven.plugins.unleash.actions;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import de.itemis.maven.plugins.unleash.util.PomUtil;
import de.itemis.maven.plugins.unleash.util.ReleaseUtil;

public class CheckAether extends AbstractProcessingAction {
  private String defaultReleaseVersion;

  public CheckAether(String defaultReleaseVersion) {
    this.defaultReleaseVersion = defaultReleaseVersion;
  }

  @Override
  public void execute() {
    List<MavenProject> alreadyReleasedProjects = Lists.newArrayList();

    for (MavenProject project : getReactorProjects()) {
      // FIXME denotes projects as released since the resolver works also locally (for mm projects)
      // TODO also check local repo (may be installed previously but not deployed
      // IDEA also do not fail on projects that have been released previously and for which the release version has been
      // entered (only snapshots)
      // QUESTION how does the release plugin behave in such a case?
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
    Optional<File> pom = getArtifactResolver().resolveArtifact(groupId, artifactId, version,
        Optional.of(PomUtil.ARTIFACT_TYPE_POM), Optional.<String> absent());
    return pom.isPresent();
  }
}
