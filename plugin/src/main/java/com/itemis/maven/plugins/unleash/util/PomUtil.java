package com.itemis.maven.plugins.unleash.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;
import com.google.common.io.Closeables;

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

  public static final String NODE_NAME_BUILD = "build";
  public static final String NODE_NAME_PLUGINS = "plugins";
  public static final String NODE_NAME_PLUGIN = "plugin";
  public static final String NODE_NAME_GROUP_ID = "groupId";
  public static final String NODE_NAME_ARTIFACT_ID = "artifactId";
  public static final String NODE_NAME_VERSION = "version";
  public static final String NODE_NAME_EXECUTIONS = "executions";
  public static final String NODE_NAME_EXECUTION = "execution";
  public static final String NODE_NAME_ID = "id";
  public static final String NODE_NAME_PHASE = "phase";
  public static final String NODE_NAME_GOALS = "goals";
  public static final String NODE_NAME_GOAL = "goal";

  public static final String getBasicCoordinates(MavenProject project) {
    StringBuilder sb = new StringBuilder(project.getGroupId()).append(':').append(project.getArtifactId()).append(':')
        .append(project.getVersion());
    return sb.toString();
  }

  private PomUtil() {
    // Should not be instanciated
  }

  public static final Document parsePOM(MavenProject project) {
    FileInputStream is = null;
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      is = new FileInputStream(project.getFile());
      Document document = documentBuilder.parse(is);
      return document;
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not load the project object model of the following module: " + getBasicCoordinates(project), e);
    } finally {
      Closeables.closeQuietly(is);
    }
  }

  public static final void writePOM(Document document, MavenProject project) {
    FileOutputStream os = null;
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      DOMSource source = new DOMSource(document);
      os = new FileOutputStream(project.getFile());
      transformer.transform(source, new StreamResult(os));
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not serialize the project object model of the following module: " + getBasicCoordinates(project), e);
    } finally {
      try {
        Closeables.close(os, true);
      } catch (IOException e) {
        throw new RuntimeException("Actually this should not happen :(", e);
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
}
