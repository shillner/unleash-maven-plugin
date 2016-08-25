package com.itemis.maven.plugins.unleash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Offers utility methods for POM parsing and writing (from/to DOM documents) as well as common POM manipulation
 * features working on DOM level.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public final class PomUtil {
  public static final String ARTIFACT_TYPE_JAR = "jar";
  public static final String ARTIFACT_TYPE_POM = "pom";
  public static final String VERSION_QUALIFIER_SNAPSHOT = "-SNAPSHOT";
  public static final String VERSION_LATEST = "LATEST";

  // DOM node names
  public static final String NODE_NAME_ARTIFACT_ID = "artifactId";
  public static final String NODE_NAME_BUILD = "build";
  public static final String NODE_NAME_EXECUTION = "execution";
  public static final String NODE_NAME_EXECUTIONS = "executions";
  public static final String NODE_NAME_GOAL = "goal";
  public static final String NODE_NAME_GOALS = "goals";
  public static final String NODE_NAME_GROUP_ID = "groupId";
  public static final String NODE_NAME_ID = "id";
  public static final String NODE_NAME_PARENT = "parent";
  public static final String NODE_NAME_PHASE = "phase";
  public static final String NODE_NAME_PLUGIN = "plugin";
  public static final String NODE_NAME_PLUGINS = "plugins";
  public static final String NODE_NAME_PROJECT = "project";
  public static final String NODE_NAME_SCM = "scm";
  public static final String NODE_NAME_SCM_CONNECTION = "connection";
  public static final String NODE_NAME_SCM_DEV_CONNECTION = "developerConnection";
  public static final String NODE_NAME_SCM_TAG = "tag";
  public static final String NODE_NAME_SCM_URL = "url";
  public static final String NODE_NAME_VERSION = "version";

  private PomUtil() {
    // Should not be instanciated
  }

  /**
   * Parses the POM file from which the project was loaded into a {@link Document} for further manipulation.
   *
   * @param project the project from which the parser retrieves the POM file.
   * @return the parsed document for further manipulation.
   */
  public static final Document parsePOM(MavenProject project) {
    try {
      return parsePOM(project.getFile());
    } catch (RuntimeException e) {
      throw new RuntimeException(
          "Could not load the project object model of the following module: " + ProjectToString.INSTANCE.apply(project),
          e);
    }
  }

  /**
   * Assumes that the passed file is a Maven POM file and parses it into a {@link Document} for further manipulation.
   *
   * @param pomFile the pom file to be parsed.
   * @return the parsed document for further manipulation.
   */
  public static final Document parsePOM(File pomFile) {
    Preconditions.checkArgument(pomFile != null && pomFile.exists() && pomFile.isFile(),
        "The project file does not exist or is invalid.");
    try {
      return parsePOM(new FileInputStream(pomFile));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Could not load the project object model from file: " + pomFile.getAbsolutePath(), e);
    }
  }

  /**
   * Assumes that the passed input Stream contains content describing a POM and parses the content into a
   * {@link Document} for further manipulation.
   *
   * @param in the stream to be parsed. This stream will be closed after parsing the document.
   * @return the parsed document for further manipulation.
   */
  public static final Document parsePOM(InputStream in) {
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      return document;
    } catch (Exception e) {
      throw new RuntimeException("Could not load the project object model from input stream.", e);
    } finally {
      Closeables.closeQuietly(in);
    }
  }

  /**
   * Serializes the passed document which should contain POM content to the project file of the passed Maven project.
   *
   * @param document the document to be serialized.
   * @param project the project from which the serialization target will be retrieved.
   */
  public static final void writePOM(Document document, MavenProject project) {
    File pom = project.getFile();
    Preconditions.checkArgument(pom != null && pom.exists() && pom.isFile(),
        "The passed project does not contain a valid POM file reference.");
    try {
      writePOM(document, new FileOutputStream(pom), true);
    } catch (Throwable t) {
      throw new RuntimeException("Could not serialize the project object model of the following module: "
          + ProjectToString.INSTANCE.apply(project), t);
    }
  }

  /**
   * Serializes the passed document which should contain POM content to the passed output stream.
   *
   * @param document the document to be serialized.
   * @param out the output stream where the document shall be written to.
   * @param closeOut whether to close the output stream afterwards or not.
   */
  public static final void writePOM(Document document, OutputStream out, boolean closeOut) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      DOMSource source = new DOMSource(document);
      transformer.transform(source, new StreamResult(out));
    } catch (Exception e) {
      throw new RuntimeException("Could not serialize the project object model to given output stream.", e);
    } finally {
      if (closeOut) {
        try {
          Closeables.close(out, true);
        } catch (IOException e) {
          throw new RuntimeException("Actually this should not happen :(", e);
        }
      }
    }
  }

  /**
   * Changes the project version of the POM as well as directly in the XML document preserving the whole document
   * formatting.
   *
   * @param model the POM where to adapt the project version.
   * @param document the POM as an XML document in which the project version shall be adapted.
   * @param newVersion the new project version to set.
   */
  public static void setProjectVersion(Model model, Document document, String newVersion) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    // if model version is null, the parent version is inherited
    if (model.getVersion() != null) {
      // first step: update the version of the in-memory project
      model.setVersion(newVersion);

      // second step: update the project version in the DOM document that is then serialized for later building
      NodeList children = document.getDocumentElement().getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (Objects.equal(child.getNodeName(), PomUtil.NODE_NAME_VERSION)) {
          child.setTextContent(newVersion);
        }
      }
    }
  }

  /**
   * Changes the project's parent version of the POM as well as directly in the XML document preserving the whole
   * document formatting.
   *
   * @param model the POM where to adapt the project's parent version.
   * @param document the POM as an XML document in which the project's parent version shall be adapted.
   * @param newVersion the new version to set for the project parent.
   */
  public static void setParentVersion(Model model, Document document, String newParentVersion) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    // first step: update parent version of the in-memory model
    Parent parent = model.getParent();
    if (parent != null) {
      parent.setVersion(newParentVersion);
    }

    // second step: update the parent version in the DOM document that will be serialized for later building
    Node parentNode = document.getDocumentElement().getElementsByTagName(PomUtil.NODE_NAME_PARENT).item(0);
    if (parentNode != null) {
      NodeList children = parentNode.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (Objects.equal(child.getNodeName(), PomUtil.NODE_NAME_VERSION)) {
          child.setTextContent(newParentVersion);
        }
      }
    }
  }

  /**
   * Queries for the project build node and creates one on demand.
   *
   * @param document the document from which to get the node or where to create the node at.
   * @param createOnDemand {@code true} if the build node shall be created when not present.
   * @return the build node of the document or {@code null} if the node doesn't exist and {@code createOnDemand} was set
   *         to {@code false}.
   */
  public static Node getOrCreateBuildNode(Document document, boolean createOnDemand) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    Node build = null;
    Element root = document.getDocumentElement();
    NodeList children = root.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (Objects.equal(NODE_NAME_BUILD, node.getNodeName())) {
        build = node;
        break;
      }
      if (build == null && createOnDemand) {
        build = document.createElement(NODE_NAME_BUILD);
        root.appendChild(build);
      }
    }

    return build;
  }

  /**
   * Queries for the project build plugins node and creates one on demand. Creation is cascaded backwards.
   *
   * @param document the document from which to get the node or where to create the node at.
   * @param createOnDemand {@code true} if the plugins node and parents shall be created when not present.
   * @return the plugins node of the document or {@code null} if the node doesn't exist and {@code createOnDemand} was
   *         set to {@code false}.
   */
  public static Node getOrCreatePluginsNode(Document document, boolean createOnDemand) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    Node build = getOrCreateBuildNode(document, createOnDemand);
    Node plugins = null;
    if (build != null) {
      NodeList children = build.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);
        if (Objects.equal(NODE_NAME_PLUGINS, node.getNodeName())) {
          plugins = node;
          break;
        }
      }
      if (plugins == null && createOnDemand) {
        plugins = document.createElement(NODE_NAME_PLUGINS);
        build.appendChild(plugins);
      }
    }

    return plugins;
  }

  /**
   * Queries the document for a specific plugin node which is identified by its groupId and artifactId.
   *
   * @param document the document from which the plugin shall be retrieved if one is configured.
   * @param groupId the groupId of the searched plugin.
   * @param artifactId the artifactId of the searched plugin.
   * @return the queried plugin node or {@code null} if none is configured.
   */
  public static Node getPlugin(Document document, String groupId, String artifactId) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    Node pluginsNode = getOrCreatePluginsNode(document, false);
    if (pluginsNode != null) {
      NodeList plugins = pluginsNode.getChildNodes();
      for (int i = 0; i < plugins.getLength(); i++) {
        Node plugin = plugins.item(i);
        if (Objects.equal(NODE_NAME_PLUGIN, plugin.getNodeName())) {
          NodeList pluginSettings = plugin.getChildNodes();
          boolean gidMatches = false;
          boolean aidMatches = false;
          for (int j = 0; j < pluginSettings.getLength(); j++) {
            Node setting = pluginSettings.item(j);
            if (Objects.equal(NODE_NAME_GROUP_ID, setting.getNodeName())) {
              if (Objects.equal(groupId, setting.getTextContent())) {
                gidMatches = true;
              }
            } else if (Objects.equal(NODE_NAME_ARTIFACT_ID, setting.getNodeName())) {
              if (Objects.equal(artifactId, setting.getTextContent())) {
                aidMatches = true;
              }
            }
            if (gidMatches && aidMatches) {
              return plugin;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Creates a plugin node in the given POM document or returns an already existing plugin node with the passed
   * coordinates.
   *
   * @param document the document where to create the plugin at.
   * @param groupId the groupId of the plugin to be created.
   * @param artifactId the artifactId of the plugin to be created.
   * @param version the version of the plugin to be created.
   * @return the node representing this plugin.
   */
  public static Node createPlugin(Document document, String groupId, String artifactId, String version) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    Node plugins = getOrCreatePluginsNode(document, true);

    Node existingPlugin = getPlugin(document, groupId, artifactId);
    if (existingPlugin != null) {
      if (!hasChildNode(existingPlugin, NODE_NAME_VERSION)) {
        Element ver = document.createElement(NODE_NAME_VERSION);
        ver.setTextContent(version);
        existingPlugin.appendChild(ver);
      }
      return existingPlugin;
    } else {
      Element plugin = document.createElement(NODE_NAME_PLUGIN);
      plugins.appendChild(plugin);

      Element gid = document.createElement(NODE_NAME_GROUP_ID);
      gid.setTextContent(groupId);
      plugin.appendChild(gid);

      Element aid = document.createElement(NODE_NAME_ARTIFACT_ID);
      aid.setTextContent(artifactId);
      plugin.appendChild(aid);

      Element ver = document.createElement(NODE_NAME_VERSION);
      ver.setTextContent(version);
      plugin.appendChild(ver);
      return plugin;
    }
  }

  /**
   * Creates a new execution element under the given plugin node.
   *
   * @param plugin the plugin under which the new execution shall be created.
   * @param id the execution id.
   * @param phase the phase in which the execution shall run.
   * @param goals the goals to be executed.
   * @return the freshly created execution element.
   */
  public static Node createPluginExecution(Node plugin, String id, Optional<String> phase, String... goals) {
    Document document = plugin.getOwnerDocument();

    Node executions = null;
    NodeList children = plugin.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (Objects.equal(NODE_NAME_EXECUTIONS, child.getNodeName())) {
        executions = child;
        break;
      }
    }
    if (executions == null) {
      executions = document.createElement(NODE_NAME_EXECUTIONS);
      plugin.appendChild(executions);
    }

    Element execution = document.createElement(NODE_NAME_EXECUTION);
    executions.appendChild(execution);

    Element idNode = document.createElement(NODE_NAME_ID);
    idNode.setTextContent(id);
    execution.appendChild(idNode);

    if (phase.isPresent()) {
      Element phaseNode = document.createElement(NODE_NAME_PHASE);
      phaseNode.setTextContent(phase.get());
      execution.appendChild(phaseNode);
    }

    if (goals.length > 0) {
      Element goalsNode = document.createElement(NODE_NAME_GOALS);
      execution.appendChild(goalsNode);

      for (String goal : goals) {
        Element goalNode = document.createElement(NODE_NAME_GOAL);
        goalNode.setTextContent(goal);
        goalsNode.appendChild(goalNode);
      }
    }

    return execution;
  }

  /**
   * Queries the document for an existing SCM node and creates one on demand if requested.
   *
   * @param document the document to query.
   * @param createOnDemand if {@code true} the node will be created on demand.
   * @return the SCM node of the document or {@code null} if the node doesn't exist and {@code createOnDemand} was set
   *         to {@code false}.
   */
  public static Node getOrCreateScmNode(Document document, boolean createOnDemand) {
    Preconditions.checkArgument(hasChildNode(document, NODE_NAME_PROJECT),
        "The document doesn't seem to be a POM model, project element is missing.");

    NodeList scmNodeList = document.getElementsByTagName(NODE_NAME_SCM);
    Node scm = null;
    if (scmNodeList.getLength() == 0 && createOnDemand) {
      scm = document.createElement(NODE_NAME_SCM);
      document.getDocumentElement().appendChild(scm);
    } else {
      scm = scmNodeList.item(0);
    }
    return scm;
  }

  /**
   * Sets the text content of the given node to the specified value preserving all whitespace.
   *
   * @param parentNode the parent node of the node where to set the text content.
   * @param nodeName the name of the node to set the text content at.
   * @param content the text content to set.
   * @param createOnDemand if {@code true} the node with name {@code nodeName} will be created as a child of
   *          {@code parentNode} if it does not exist.
   */
  public static void setNodeTextContent(Node parentNode, String nodeName, String content, boolean createOnDemand) {
    Node node = null;
    NodeList children = parentNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (Objects.equal(nodeName, n.getNodeName())) {
        node = n;
        break;
      }
    }

    if (node == null && createOnDemand) {
      node = parentNode.getOwnerDocument().createElement(nodeName);
      if (children.getLength() > 0) {
        Node lastChild = children.item(children.getLength() - 1);
        Text lineBreak = parentNode.getOwnerDocument().createTextNode("\n");
        parentNode.insertBefore(lineBreak, lastChild);
        parentNode.insertBefore(node, lastChild);
      } else {
        parentNode.appendChild(node);
      }
    }

    if (node != null) {
      node.setTextContent(content);
    }
  }

  /**
   * Deletes the specified node from the given parent.
   * 
   * @param parentNode the parent of the node to delete.
   * @param nodeName the name of the node to delete.
   */
  public static void deleteNode(Node parentNode, String nodeName) {
    Node nodeToDelete = null;
    NodeList children = parentNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (Objects.equal(nodeName, n.getNodeName())) {
        nodeToDelete = n;
        break;
      }
    }

    if (nodeToDelete != null) {
      parentNode.removeChild(nodeToDelete);
    }
  }

  /**
   * Queries a node for a child with a specific name.
   * 
   * @param parentNode the parent to query for the node.
   * @param nodeName the name of the searched node.
   * @return {@code true} if the parent contains a node with the specified name.
   */
  public static boolean hasChildNode(Node parentNode, String nodeName) {
    NodeList children = parentNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (Objects.equal(nodeName, n.getNodeName())) {
        return true;
      }
    }
    return false;
  }
}
