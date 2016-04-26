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

import com.google.common.io.Closeables;

/**
 * Some common constants regarding the POM.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public final class PomUtil {
  private PomUtil() {
    // Should not be instanciated
  }

  public static final String VERSION_QUALIFIER_SNAPSHOT = "-SNAPSHOT";

  public static final String ARTIFACT_TYPE_POM = "pom";
  public static final String ARTIFACT_TYPE_JAR = "jar";

  public static final String getBasicCoordinates(MavenProject project) {
    StringBuilder sb = new StringBuilder(project.getGroupId()).append(':').append(project.getArtifactId()).append(':')
        .append(project.getVersion());
    return sb.toString();
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
}
