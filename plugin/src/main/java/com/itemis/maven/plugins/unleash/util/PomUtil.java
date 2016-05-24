package com.itemis.maven.plugins.unleash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

/**
 * Some common constants regarding the POM.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public final class PomUtil {
  public static final String ARTIFACT_TYPE_JAR = "jar";
  public static final String ARTIFACT_TYPE_POM = "pom";
  public static final String VERSION_QUALIFIER_SNAPSHOT = "-SNAPSHOT";
  public static final String VERSION_LATEST = "LATEST";

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
  public static final String NODE_NAME_SCM = "scm";
  public static final String NODE_NAME_SCM_CONNECTION = "connection";
  public static final String NODE_NAME_SCM_DEV_CONNECTION = "developerConnection";
  public static final String NODE_NAME_SCM_TAG = "tag";
  public static final String NODE_NAME_VERSION = "version";

  private PomUtil() {
    // Should not be instanciated
  }

  public static final Document parsePOM(MavenProject project) {
    try {
      return parsePOM(project.getFile());
    } catch (RuntimeException e) {
      throw new RuntimeException(
          "Could not load the project object model of the following module: " + ProjectToString.INSTANCE.apply(project),
          e);
    }
  }

  public static final Document parsePOM(File f) {
    FileInputStream is = null;
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      is = new FileInputStream(f);
      Document document = documentBuilder.parse(is);
      return document;
    } catch (Exception e) {
      throw new RuntimeException("Could not load the project object model from file: " + f.getAbsolutePath(), e);
    } finally {
      Closeables.closeQuietly(is);
    }
  }

  public static final void writePOM(Document document, MavenProject project) {
    try {
      writePOM(document, new FileOutputStream(project.getFile()), true);
    } catch (Throwable t) {
      throw new RuntimeException("Could not serialize the project object model of the following module: "
          + ProjectToString.INSTANCE.apply(project), t);
    }
  }

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

  public static void setProjectVersion(Model model, Document document, String newVersion) {
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

  public static void setParentVersion(Model model, Document document, String newParentVersion) {
    Parent parent = model.getParent();
    // first step: update parent version of the in-memory model
    parent.setVersion(newParentVersion);

    // second step: update the parent version in the DOM document that will be serialized for later building
    Node parentNode = document.getDocumentElement().getElementsByTagName(PomUtil.NODE_NAME_PARENT).item(0);
    NodeList children = parentNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (Objects.equal(child.getNodeName(), PomUtil.NODE_NAME_VERSION)) {
        child.setTextContent(newParentVersion);
      }
    }
  }

  public static Node getOrCreateBuildNode(Document document, boolean createOnDemand) {
    NodeList buildNodeList = document.getElementsByTagName(NODE_NAME_BUILD);
    Node build = null;
    if (buildNodeList.getLength() == 0 && createOnDemand) {
      build = document.createElement(NODE_NAME_BUILD);
      document.appendChild(build);
    } else {
      build = buildNodeList.item(0);
    }
    return build;
  }

  public static Node getOrCreatePluginsNode(Document document, boolean createOnDemand) {
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

  public static Node getPlugin(Document document, String groupId, String artifactId) {
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

  public static Node createPlugin(Document document, String groupId, String artifactId, String version) {
    Node plugins = getOrCreatePluginsNode(document, true);

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

  public static Node createPluginExecution(Node plugin, String id, String phase, String... goals) {
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

    Element phaseNode = document.createElement(NODE_NAME_PHASE);
    phaseNode.setTextContent(phase);
    execution.appendChild(phaseNode);

    Element goalsNode = document.createElement(NODE_NAME_GOALS);
    execution.appendChild(goalsNode);

    for (String goal : goals) {
      Element goalNode = document.createElement(NODE_NAME_GOAL);
      goalNode.setTextContent(goal);
      goalsNode.appendChild(goalNode);
    }

    return execution;
  }

  public static Node getOrCreateScmNode(Document document, boolean createOnDemand) {
    NodeList scmNodeList = document.getElementsByTagName(NODE_NAME_SCM);
    Node scm = null;
    if (scmNodeList.getLength() == 0 && createOnDemand) {
      scm = document.createElement(NODE_NAME_SCM);
      document.appendChild(scm);
    } else {
      scm = scmNodeList.item(0);
    }
    return scm;
  }

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
