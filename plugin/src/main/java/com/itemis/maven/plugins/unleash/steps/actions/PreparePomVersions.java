package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@ProcessingStep(@Goal(name = "perform", stepNumber = 40))
public class PreparePomVersions implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  private ReleaseMetadata metadata;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    for (MavenProject project : this.reactorProjects) {
      updateProjectVersion(project);
      updateParentVersion(project);

      try {
        // loads a plain model from the project file since project.getModel() would return a pom with mixed-in parent
        // declarations and would thus not be suitable for writing back
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new InputStreamReader(new FileInputStream(project.getFile())));
        if (model.getVersion() != null) {
          model.setVersion(project.getVersion());
        }
        if (model.getParent() != null) {
          model.getParent().setVersion(project.getParent().getVersion());
        }

        // TODO maybe do not just serialize the model but adapt the versions by hand to preserve the exact formatting of
        // the pom
        new DefaultModelWriter().write(new File(project.getFile().getAbsolutePath().concat("_new")), null, model);
      } catch (XmlPullParserException e) {
        throw new MojoExecutionException(
            "Unable to load POM for version modification: " + project.getGroupId() + ":" + project.getArtifactId(), e);
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to serialize POM with adapted release versions: "
            + project.getGroupId() + ":" + project.getArtifactId(), e);
      }
    }
  }

  private void updateProjectVersion(MavenProject project) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    ArtifactCoordinates pre = coordinatesByPhase.get(ReleasePhase.PRE);
    ArtifactCoordinates post = coordinatesByPhase.get(ReleasePhase.POST);

    Model model = project.getModel();
    // if model version is null, the parent version is inherited
    if (model.getVersion() != null) {
      model.setVersion(post.getVersion());
      this.log.debug("Prepared release version of module '" + project.getGroupId() + ":" + project.getArtifact() + "': "
          + pre.getVersion() + " => " + post.getVersion());
    }
  }

  private void updateParentVersion(MavenProject project) {
    Model model = project.getModel();
    Parent parent = model.getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates pre = coordinatesByPhase.get(ReleasePhase.PRE);
      ArtifactCoordinates post = coordinatesByPhase.get(ReleasePhase.POST);

      if (post != null) {
        // null indicates that the parent is not part of the reactor projcets since no release version had been
        // calculated for it
        parent.setVersion(post.getVersion());
        this.log.debug("Adapted the parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "': " + pre.getVersion() + " => " + post.getVersion());
      }
    }
  }
}
