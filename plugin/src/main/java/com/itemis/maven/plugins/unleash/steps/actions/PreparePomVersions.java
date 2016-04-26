package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;

@ProcessingStep(@Goal(name = "perform", stepNumber = 40))
public class PreparePomVersions implements CDIMojoProcessingStep {
  private static final String NODE_NAME_VERSION = "version";
  private static final String NODE_NAME_PARENT = "parent";

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
      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document, ReleasePhase.POST);
        setParentVersion(project, document, ReleasePhase.POST);
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for release.", t);
      }
    }
  }

  @RollbackOnError
  public void rollback() {
    this.log.info("Rolling back POM version modifiactions due to a processing exception.");
    for (MavenProject project : this.reactorProjects) {
      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document, ReleasePhase.PRE);
        setParentVersion(project, document, ReleasePhase.PRE);
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new RuntimeException("Could not reset versions after failed release.", t);
      }
    }
  }

  private void setProjectVersion(MavenProject project, Document document, ReleasePhase phase) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVerion = null;
    String newVersion = null;
    switch (phase) {
      case POST:
        oldVerion = coordinatesByPhase.get(ReleasePhase.PRE).getVersion();
        newVersion = coordinatesByPhase.get(ReleasePhase.POST).getVersion();
        break;
      case PRE:
        oldVerion = coordinatesByPhase.get(ReleasePhase.POST).getVersion();
        newVersion = coordinatesByPhase.get(ReleasePhase.PRE).getVersion();
        break;
    }

    Model model = project.getModel();
    // if model version is null, the parent version is inherited
    if (model.getVersion() != null) {
      // first step: update the version of the in-memory project
      model.setVersion(newVersion);

      // second step: update the project version in the DOM document that is then serialized for later building
      NodeList children = document.getDocumentElement().getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (Objects.equal(child.getNodeName(), NODE_NAME_VERSION)) {
          child.setTextContent(newVersion);
        }
      }

      this.log.info("Update of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' ["
          + oldVerion + " => " + newVersion + "]");
    }
  }

  private void setParentVersion(MavenProject project, Document document, ReleasePhase phase) {
    Model model = project.getModel();
    Parent parent = model.getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates oldCoordinates = null;
      ArtifactCoordinates newCoordinates = null;
      switch (phase) {
        case POST:
          oldCoordinates = coordinatesByPhase.get(ReleasePhase.PRE);
          newCoordinates = coordinatesByPhase.get(ReleasePhase.POST);
          break;
        case PRE:
          oldCoordinates = coordinatesByPhase.get(ReleasePhase.POST);
          newCoordinates = coordinatesByPhase.get(ReleasePhase.PRE);
          break;
      }

      // null indicates that the parent is not part of the reactor projects since no release version had been calculated
      // for it
      if (newCoordinates != null) {
        // first step: update parent version of the in-memory model
        parent.setVersion(newCoordinates.getVersion());

        // second step: update the parent version in the DOM document that will be serialized for later building
        Node parentNode = document.getDocumentElement().getElementsByTagName(NODE_NAME_PARENT).item(0);
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
          Node child = children.item(i);
          if (Objects.equal(child.getNodeName(), NODE_NAME_VERSION)) {
            child.setTextContent(newCoordinates.getVersion());
          }
        }

        this.log.info("Update of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
      }
    }
  }
}
