package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
      updateProjectVersion(project);
      updateParentVersion(project);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        Document document = documentBuilder.parse(new FileInputStream(project.getFile()));
        Element root = document.getDocumentElement();

        // updates the project version if the project declares the version
        if (project.getModel().getVersion() != null) {
          NodeList children = root.getChildNodes();
          for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (Objects.equal(child.getNodeName(), NODE_NAME_VERSION)) {
              child.setTextContent(project.getVersion());
            }
          }
        }

        // updates the parent version if one is set
        if (project.getParent() != null) {
          Node parentNode = root.getElementsByTagName(NODE_NAME_PARENT).item(0);
          NodeList children = parentNode.getChildNodes();
          for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (Objects.equal(child.getNodeName(), NODE_NAME_VERSION)) {
              child.setTextContent(project.getParent().getVersion());
            }
          }
        }

        // IDEA outsource pom parsing and writing for further tasks
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new FileOutputStream(project.getFile()));
        transformer.transform(source, result);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for release.", t);
      }
    }
  }

  @RollbackOnError
  public void rollback(Exception e) {
    // TODO implement!
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
