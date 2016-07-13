package com.itemis.maven.plugins.unleash.steps.checks;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.predicates.IsSnapshotProject;

/**
 * Checks that none of the project modules references a parent with a SNAPSHOT version.<br>
 * If a module references a SNAPSHOT parent that is also scheduled for release the process will resume.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "checkParentVersions", description = "Checks that none of the project modules references a SNAPSHOT parent that is not scheduled for release.", requiresOnline = false)
public class CheckParentVersions implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info(
        "Checking that the project modules do not reference SNAPSHOT parents that are not scheduled for release.");

    Map<String, String> snapshotParentReferences = Maps.newHashMap();
    for (MavenProject p : this.reactorProjects) {
      MavenProject parent = p.getParent();
      if (parent != null && IsSnapshotProject.INSTANCE.apply(parent)) {
        Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
            .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
        ArtifactCoordinates coordinates = coordinatesByPhase.get(ReleasePhase.PRE_RELEASE);
        if (Objects.equal(parent.getVersion(), coordinates.getVersion())) {
          this.log.debug("\tModule '" + ProjectToString.INSTANCE.apply(p) + "' references SNAPSHOT parent '"
              + ProjectToString.INSTANCE.apply(parent) + "' which is also scheduled for release.");
        } else {
          snapshotParentReferences.put(ProjectToString.INSTANCE.apply(p), ProjectToString.INSTANCE.apply(parent));
        }
      }
    }

    if (!snapshotParentReferences.isEmpty()) {
      this.log.error("\tThe following modules have references to SNAPSHOT parents that are not scheduled for release:");
      for (String projectCoordinates : snapshotParentReferences.keySet()) {
        this.log.error("\t\t" + projectCoordinates + " => " + snapshotParentReferences.get(projectCoordinates));
      }
      throw new MojoFailureException(
          "There are modules that reference SNAPSHOT parents which are not scheduled for release!");
    }
  }
}
