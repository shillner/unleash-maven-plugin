package com.itemis.maven.plugins.unleash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.io.Closeables;

public class PomUtilTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder TemporaryFolder = new TemporaryFolder();

  @Test
  public void testParsePOM() {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File f;
    try {
      f = new File(url.toURI());
    } catch (URISyntaxException e) {
      f = new File(url.getPath());
    }

    Optional<Document> parsedPOM = PomUtil.parsePOM(f);
    Assert.assertTrue(parsedPOM.isPresent());
    Document document = parsedPOM.get();
    Assert.assertNotNull(document);
    Element root = document.getDocumentElement();
    Assert.assertEquals("project", root.getTagName());
    Node gid = getNode(root, PomUtil.NODE_NAME_GROUP_ID);
    Assert.assertNotNull(gid);
    Assert.assertEquals("com.itemis.maven.plugins", gid.getTextContent());
    Assert.assertEquals("test", document.getElementsByTagName("finalName").item(0).getTextContent());

    FileInputStream is;
    try {
      is = new FileInputStream(f);
      document = PomUtil.parsePOM(is);
      Assert.assertNotNull(document);
      root = document.getDocumentElement();
      Assert.assertEquals("project", root.getTagName());
      gid = getNode(root, PomUtil.NODE_NAME_GROUP_ID);
      Assert.assertNotNull(gid);
      Assert.assertEquals("com.itemis.maven.plugins", gid.getTextContent());
      Assert.assertEquals("test", document.getElementsByTagName("finalName").item(0).getTextContent());
    } catch (IOException e) {
      Assert.fail("Could not open Stream from POM file");
    }

    MavenProject project = new MavenProject();
    project.setFile(f);
    parsedPOM = PomUtil.parsePOM(project);
    Assert.assertTrue(parsedPOM.isPresent());
    document = parsedPOM.get();
    Assert.assertNotNull(document);
    root = document.getDocumentElement();
    Assert.assertEquals("project", root.getTagName());
    gid = getNode(root, PomUtil.NODE_NAME_GROUP_ID);
    Assert.assertNotNull(gid);
    Assert.assertEquals("com.itemis.maven.plugins", gid.getTextContent());
    Assert.assertEquals("test", document.getElementsByTagName("finalName").item(0).getTextContent());
  }

  @Test
  public void testParsePOM_Invalid() {
    this.exception.expect(RuntimeException.class);
    this.exception.expectCause(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        return item instanceof SAXParseException;
      }

      @Override
      public void describeTo(Description description) {
      }
    });

    URL url = getClass().getResource(getClass().getSimpleName() + "/pom2.xml");
    File f;
    try {
      f = new File(url.toURI());
    } catch (URISyntaxException e) {
      f = new File(url.getPath());
    }

    PomUtil.parsePOM(f);
    Assert.fail("Parser should throw an exception since the document is not well formed!");
  }

  @Test
  public void testWritePOM_Project() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    File f = this.TemporaryFolder.newFile();
    MavenProject project = new MavenProject();
    project.setFile(f);
    PomUtil.writePOM(document, project);

    Document parsedDocument = builder.parse(f);
    Assert.assertNotNull(parsedDocument);
    Element root = parsedDocument.getDocumentElement();
    Assert.assertEquals("project", root.getTagName());
    Node gid = getNode(root, PomUtil.NODE_NAME_GROUP_ID);
    Assert.assertNotNull(gid);
    Assert.assertEquals("com.itemis.maven.plugins", gid.getTextContent());
    Node aid = getNode(root, PomUtil.NODE_NAME_ARTIFACT_ID);
    Assert.assertNotNull(aid);
    Assert.assertEquals("test-project-1", aid.getTextContent());
    Assert.assertEquals("1", parsedDocument.getElementsByTagName(PomUtil.NODE_NAME_VERSION).item(0).getTextContent());
  }

  @Test
  public void testWritePOM_Stream() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    File f = this.TemporaryFolder.newFile();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      PomUtil.writePOM(document, fos, true);
      try {
        fos.write(1);
        Assert.fail("Write method did not close the output stream although it had been requested.");
      } catch (Exception e) {
        // nothing to do here
      }
    } finally {
      Closeables.close(fos, true);
    }

    Document parsedDocument = builder.parse(f);
    Assert.assertNotNull(parsedDocument);
    Element root = parsedDocument.getDocumentElement();
    Assert.assertEquals("project", root.getTagName());
    Node gid = getNode(root, PomUtil.NODE_NAME_GROUP_ID);
    Assert.assertNotNull(gid);
    Assert.assertEquals("com.itemis.maven.plugins", gid.getTextContent());
    Node aid = getNode(root, PomUtil.NODE_NAME_ARTIFACT_ID);
    Assert.assertNotNull(aid);
    Assert.assertEquals("test-project-1", aid.getTextContent());
    Assert.assertEquals("1", parsedDocument.getElementsByTagName(PomUtil.NODE_NAME_VERSION).item(0).getTextContent());

    f = this.TemporaryFolder.newFile();
    fos = null;
    try {
      fos = new FileOutputStream(f);
      PomUtil.writePOM(document, fos, false);
      try {
        fos.write(1);
      } catch (Exception e) {
        Assert.fail("Write method did not close the output stream although it had been requested.");
      }
    } finally {
      Closeables.close(fos, true);
    }
  }

  @Test
  public void testSetProjectVersion() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    Model model = new Model();
    model.setGroupId("com.itemis.maven.plugins");
    model.setArtifactId("test-project-1");
    model.setVersion("1");

    String newVersion = "2-SNAPSHOT";
    PomUtil.setProjectVersion(model, document, newVersion);
    Assert.assertEquals(newVersion, getNode(document.getDocumentElement(), PomUtil.NODE_NAME_VERSION).getTextContent());
    Assert.assertEquals(newVersion, model.getVersion());
  }

  @Test
  public void testSetProjectVersion_notPresent() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom3.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    Model model = new Model();
    model.setGroupId("com.itemis.maven.plugins");
    model.setArtifactId("test-project-1");

    String newVersion = "2-SNAPSHOT";
    PomUtil.setProjectVersion(model, document, newVersion);
    Assert.assertNull(getNode(document.getDocumentElement(), PomUtil.NODE_NAME_VERSION));
    Assert.assertNull(newVersion, model.getVersion());
  }

  @Test
  public void testSetParentVersion() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    Model model = new Model();
    model.setGroupId("com.itemis.maven.plugins");
    model.setArtifactId("test-project-1");
    model.setVersion("1");
    Parent parent = new Parent();
    parent.setGroupId("com.itemis");
    parent.setArtifactId("org-parent");
    parent.setVersion("1");
    model.setParent(parent);

    String newVersion = "2";
    PomUtil.setParentVersion(model, document, newVersion);

    Assert.assertEquals(newVersion, getNode(document.getDocumentElement(), "parent/version").getTextContent());
    Assert.assertEquals(newVersion, model.getParent().getVersion());
  }

  @Test
  public void testGetOrCreateBuildNode() throws Exception {
    String nodePath = "project/build";

    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNotNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreateBuildNode(document, false));
    Assert.assertNotNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreateBuildNode(document, true));
    Assert.assertNotNull(getNode(document, nodePath));

    // missing build node!
    url = getClass().getResource(getClass().getSimpleName() + "/pom3.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNull(PomUtil.getOrCreateBuildNode(document, false));
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreateBuildNode(document, true));
    Assert.assertNotNull(getNode(document, nodePath));
  }

  @Test
  public void testGetOrCreatePluginsNode() throws Exception {
    String nodePath = "project/build/plugins";

    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    // missing plugins node
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNull(PomUtil.getOrCreatePluginsNode(document, false));
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreatePluginsNode(document, true));
    Assert.assertNotNull(getNode(document, nodePath));

    // missing build node!
    url = getClass().getResource(getClass().getSimpleName() + "/pom3.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNull(PomUtil.getOrCreatePluginsNode(document, false));
    Assert.assertNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreatePluginsNode(document, true));
    Assert.assertNotNull(getNode(document, nodePath));

    // plugins node present
    url = getClass().getResource(getClass().getSimpleName() + "/pom4.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    Assert.assertNotNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreatePluginsNode(document, false));
    Assert.assertNotNull(getNode(document, nodePath));
    Assert.assertNotNull(PomUtil.getOrCreatePluginsNode(document, true));
    Assert.assertNotNull(getNode(document, nodePath));
  }

  @Test
  public void testGetPlugin() throws Exception {
    String pluginsNodePath = "project/build/plugins";
    String gid = "org.apache.maven.plugins";
    String aid = "maven-compiler-plugin";

    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    // missing plugins node
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNull(getNode(document, pluginsNodePath));
    Assert.assertNull(PomUtil.getPlugin(document, gid, aid));

    // missing build node!
    url = getClass().getResource(getClass().getSimpleName() + "/pom3.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    Assert.assertNull(getNode(document, pluginsNodePath));
    Assert.assertNull(PomUtil.getPlugin(document, gid, aid));

    // plugin present
    url = getClass().getResource(getClass().getSimpleName() + "/pom4.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    Node plugins = getNode(document, pluginsNodePath);
    Assert.assertNotNull(plugins);
    Node plugin = null;
    for (int i = 0; i < plugins.getChildNodes().getLength(); i++) {
      Node child = plugins.getChildNodes().item(i);
      if (Objects.equal(PomUtil.NODE_NAME_PLUGIN, child.getNodeName())) {
        if (Objects.equal(gid, getNode(child, PomUtil.NODE_NAME_GROUP_ID).getTextContent())
            && Objects.equal(aid, getNode(child, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent())) {
          plugin = child;
          break;
        }
      }
    }
    Assert.assertNotNull(plugin);
    Node pluginFromUtil = PomUtil.getPlugin(document, gid, aid);
    Assert.assertNotNull(pluginFromUtil);
    Assert.assertEquals(gid, getNode(pluginFromUtil, PomUtil.NODE_NAME_GROUP_ID).getTextContent());
    Assert.assertEquals(aid, getNode(pluginFromUtil, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent());
  }

  @Test
  public void testCreatePlugin() throws Exception {
    String pluginsNodePath = "project/build/plugins";
    String gid = "org.apache.maven.plugins";
    String aid = "maven-compiler-plugin";
    String version = "3.5.1";

    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    // missing plugins node
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNull(getNode(document, pluginsNodePath));
    Node plugin = PomUtil.createPlugin(document, gid, aid, version);
    Assert.assertNotNull(plugin);
    Node plugins = getNode(document, pluginsNodePath);
    Node p = null;
    for (int i = 0; i < plugins.getChildNodes().getLength(); i++) {
      Node child = plugins.getChildNodes().item(i);
      if (Objects.equal(PomUtil.NODE_NAME_PLUGIN, child.getNodeName())) {
        if (Objects.equal(gid, getNode(child, PomUtil.NODE_NAME_GROUP_ID).getTextContent())
            && Objects.equal(aid, getNode(child, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent())) {
          p = child;
          break;
        }
      }
    }
    Assert.assertNotNull(p);
    Assert.assertEquals(gid, getNode(p, PomUtil.NODE_NAME_GROUP_ID).getTextContent());
    Assert.assertEquals(aid, getNode(p, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent());
    Assert.assertEquals(version, getNode(p, PomUtil.NODE_NAME_VERSION).getTextContent());

    // plugin present
    url = getClass().getResource(getClass().getSimpleName() + "/pom4.xml");
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    document = builder.parse(source);
    plugins = getNode(document, pluginsNodePath);
    Assert.assertNotNull(plugins);
    plugin = null;
    for (int i = 0; i < plugins.getChildNodes().getLength(); i++) {
      Node child = plugins.getChildNodes().item(i);
      if (Objects.equal(PomUtil.NODE_NAME_PLUGIN, child.getNodeName())) {
        if (Objects.equal(gid, getNode(child, PomUtil.NODE_NAME_GROUP_ID).getTextContent())
            && Objects.equal(aid, getNode(child, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent())) {
          plugin = child;
          break;
        }
      }
    }
    Assert.assertNotNull(plugin);
    Assert.assertNull(getNode(plugin, PomUtil.NODE_NAME_VERSION));
    int numPlugins = plugins.getChildNodes().getLength();

    Node pluginFromUtil = PomUtil.createPlugin(document, gid, aid, version);
    Assert.assertNotNull(pluginFromUtil);
    Assert.assertEquals(gid, getNode(pluginFromUtil, PomUtil.NODE_NAME_GROUP_ID).getTextContent());
    Assert.assertEquals(aid, getNode(pluginFromUtil, PomUtil.NODE_NAME_ARTIFACT_ID).getTextContent());
    Assert.assertEquals(version, getNode(pluginFromUtil, PomUtil.NODE_NAME_VERSION).getTextContent());

    plugins = getNode(document, pluginsNodePath);
    Assert.assertEquals(numPlugins, plugins.getChildNodes().getLength());
  }

  @Test
  public void testCreatePluginExecution() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom4.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);

    // plugin without executions node -> shall be created before execution is created
    Node plugin = PomUtil.getPlugin(document, "org.apache.maven.plugins", "maven-javadoc-plugin");
    if (plugin == null) {
      Assert.fail("Expected the plugin node to be present.");
    }
    PomUtil.createPluginExecution(plugin, "123", Optional.of("verify"), "test");
    Node executions = getNode(plugin, PomUtil.NODE_NAME_EXECUTIONS);
    Assert.assertNotNull(executions);
    Node execution = executions.getFirstChild();
    Assert.assertNotNull(execution);
    Node id = getNode(execution, PomUtil.NODE_NAME_ID);
    Assert.assertNotNull(id);
    Assert.assertEquals("123", id.getTextContent());
    Node phase = getNode(execution, PomUtil.NODE_NAME_PHASE);
    Assert.assertNotNull(phase);
    Assert.assertEquals("verify", phase.getTextContent());
    Node goals = getNode(execution, PomUtil.NODE_NAME_GOALS);
    Assert.assertNotNull(goals);
    Assert.assertEquals(1, goals.getChildNodes().getLength());
    Node goal = goals.getFirstChild();
    Assert.assertNotNull(goal);
    Assert.assertEquals("test", goal.getTextContent());

    // plugin with executions node being present
    plugin = PomUtil.getPlugin(document, "x.y.z", "test");
    if (plugin == null) {
      Assert.fail("Expected the plugin node to be present.");
    }
    PomUtil.createPluginExecution(plugin, "123", Optional.of("verify"), "test");
    executions = getNode(plugin, PomUtil.NODE_NAME_EXECUTIONS);
    Assert.assertNotNull(executions);
    execution = executions.getLastChild();
    Assert.assertNotNull(execution);
    id = getNode(execution, PomUtil.NODE_NAME_ID);
    Assert.assertNotNull(id);
    Assert.assertEquals("123", id.getTextContent());
    phase = getNode(execution, PomUtil.NODE_NAME_PHASE);
    Assert.assertNotNull(phase);
    Assert.assertEquals("verify", phase.getTextContent());
    goals = getNode(execution, PomUtil.NODE_NAME_GOALS);
    Assert.assertNotNull(goals);
    Assert.assertEquals(1, goals.getChildNodes().getLength());
    goal = goals.getFirstChild();
    Assert.assertNotNull(goal);
    Assert.assertEquals("test", goal.getTextContent());

    // creates an execution without a phase (default phase)
    PomUtil.createPluginExecution(plugin, "123", Optional.<String> absent(), "test");
    executions = getNode(plugin, PomUtil.NODE_NAME_EXECUTIONS);
    Assert.assertNotNull(executions);
    execution = executions.getLastChild();
    Assert.assertNotNull(execution);
    id = getNode(execution, PomUtil.NODE_NAME_ID);
    Assert.assertNotNull(id);
    Assert.assertEquals("123", id.getTextContent());
    phase = getNode(execution, PomUtil.NODE_NAME_PHASE);
    Assert.assertNull(phase);
    goals = getNode(execution, PomUtil.NODE_NAME_GOALS);
    Assert.assertNotNull(goals);
    Assert.assertEquals(1, goals.getChildNodes().getLength());
    goal = goals.getFirstChild();
    Assert.assertNotNull(goal);
    Assert.assertEquals("test", goal.getTextContent());

    // creates an execution without goals
    PomUtil.createPluginExecution(plugin, "123", Optional.of("verify"));
    executions = getNode(plugin, PomUtil.NODE_NAME_EXECUTIONS);
    Assert.assertNotNull(executions);
    execution = executions.getLastChild();
    Assert.assertNotNull(execution);
    id = getNode(execution, PomUtil.NODE_NAME_ID);
    Assert.assertNotNull(id);
    Assert.assertEquals("123", id.getTextContent());
    phase = getNode(execution, PomUtil.NODE_NAME_PHASE);
    Assert.assertNotNull(phase);
    Assert.assertEquals("verify", phase.getTextContent());
    Assert.assertNull(getNode(execution, PomUtil.NODE_NAME_GOALS));
  }

  @Test
  public void testGetOrCreateScmNode() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Node scm = getNode(document, "project/scm");
    Assert.assertNull(scm);
    scm = PomUtil.getOrCreateScmNode(document, false);
    Assert.assertNull(scm);
    scm = PomUtil.getOrCreateScmNode(document, true);
    Assert.assertNotNull(scm);
  }

  @Test
  public void testSetNodeTextContent() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Node name = getNode(document, "project/name");
    Assert.assertNotNull(name);
    Assert.assertEquals("TEST", name.getTextContent());
    PomUtil.setNodeTextContent(document.getDocumentElement(), "name", "XYZ", false);
    Assert.assertEquals("XYZ", name.getTextContent());

    Node description = getNode(document, "project/description");
    Assert.assertNull(description);
    PomUtil.setNodeTextContent(document.getDocumentElement(), "description", "XYZ", false);
    description = getNode(document, "project/description");
    Assert.assertNull(description);
    PomUtil.setNodeTextContent(document.getDocumentElement(), "description", "XYZ", true);
    description = getNode(document, "project/description");
    Assert.assertNotNull(description);
    Assert.assertEquals("XYZ", description.getTextContent());
  }

  @Test
  public void testDeleteNode() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNotNull(getNode(document, "project/name"));
    PomUtil.deleteNode(document.getDocumentElement(), "name");
    Assert.assertNull(getNode(document, "project/name"));
  }

  @Test
  public void testHasChildNode() throws Exception {
    URL url = getClass().getResource(getClass().getSimpleName() + "/pom1.xml");
    File source;
    try {
      source = new File(url.toURI());
    } catch (URISyntaxException e) {
      source = new File(url.getPath());
    }

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(source);
    Assert.assertNotNull(getNode(document, "project/name"));
    Assert.assertTrue(PomUtil.hasChildNode(document.getDocumentElement(), "name"));
    Assert.assertNull(getNode(document, "project/description"));
    Assert.assertFalse(PomUtil.hasChildNode(document.getDocumentElement(), "description"));
  }

  private Node getNode(Node parent, String name) {
    String nodeName = name;
    String childPath = null;
    int separatorIndex = name.indexOf('/');
    if (separatorIndex >= 0) {
      nodeName = name.substring(0, separatorIndex);
      childPath = name.substring(separatorIndex + 1);
    }

    NodeList childNodes = parent.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node n = childNodes.item(i);
      if (Objects.equal(n.getNodeName(), nodeName)) {
        if (childPath != null) {
          return getNode(n, childPath);
        }
        return n;
      }
    }
    return null;
  }
}
