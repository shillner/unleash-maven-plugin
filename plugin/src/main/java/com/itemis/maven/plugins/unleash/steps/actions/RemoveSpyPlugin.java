package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.PomUtil;

/**
 * Removes the artifact-spy-plugin from the reactor pom of the project.<br>
 * This plugin helps detecting all artifacts that are produced by the release build which enables us to shift the actual
 * installation and deployment of the artifacts to the very end of the workflow. This is necessary since a
 * deployment of artifacts cannot be undone programmatically.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "removeSpyPlugin", description = "Removes the artifact-spy-plugin from the build configuration if one is configured.", requiresOnline = false)
public class RemoveSpyPlugin implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  @Named("artifactSpyPlugin")
  private ArtifactCoordinates artifactSpyPluginCoordinates;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Removing artifact-spy-plugin from build configuration.");
    try {
      for (MavenProject p : this.reactorProjects) {
        Document document = PomUtil.parsePOM(p);

        Node plugin = PomUtil.getPlugin(document, this.artifactSpyPluginCoordinates.getGroupId(),
            this.artifactSpyPluginCoordinates.getArtifactId());
        if (plugin != null) {
          Node plugins = plugin.getParentNode();
          plugins.removeChild(plugin);
          if (plugins.getChildNodes().getLength() == 0) {
            Node build = plugins.getParentNode();
            build.removeChild(plugins);
            if (build.getChildNodes().getLength() == 0) {
              build.getParentNode().removeChild(build);
            }
          }

          PomUtil.writePOM(document, p);
        }
      }
    } catch (Throwable t) {
      throw new MojoFailureException("Could not remove the artifact-spy-plugin from the POM.", t);
    }
  }
}
