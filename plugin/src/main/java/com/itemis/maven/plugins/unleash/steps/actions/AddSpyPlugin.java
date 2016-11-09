package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.PomUtil;

/**
 * Adds the artifact-spy-plugin to the reactor pom of the project.<br>
 * This plugin helps detecting all artifacts that are produced by the release build which enables us to shift the actual
 * installation and deployment of the artifacts to the very end of the workflow. This is necessary since a
 * deployment of artifacts cannot be undone programmatically.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "addSpyPlugin", description = "Adds the artifact-spy-plugin to the build configuration. This plugin detects all artifacts that are produced during the release build for later installation and deployment.", requiresOnline = false)
public class AddSpyPlugin implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  @Named("artifactSpyPlugin")
  private ArtifactCoordinates artifactSpyPluginCoordinates;
  private Map<MavenProject, Document> cachedPOMs;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info(
        "Adding artifact-spy-plugin to the build configuration. This plugin is required to detect all artifacts that are produced by the release build for later installation and deployment.");
    this.cachedPOMs = Maps.newHashMap();
    for (MavenProject p : this.reactorProjects) {
      this.cachedPOMs.put(p, PomUtil.parsePOM(p));
    }

    try {
      for (MavenProject p : this.reactorProjects) {
        Document document = PomUtil.parsePOM(p);
        Node plugin = PomUtil.createPlugin(document, this.artifactSpyPluginCoordinates.getGroupId(),
            this.artifactSpyPluginCoordinates.getArtifactId(), this.artifactSpyPluginCoordinates.getVersion());
        PomUtil.createPluginExecution(plugin, "spy", Optional.of("verify"), "spy");
        PomUtil.writePOM(document, p);
      }
    } catch (Throwable t) {
      throw new MojoFailureException(
          "Could not add the artifact-spy-plugin to the POM. This plugin is required to determine the artifacts that are produced by the build for later installation and deployment.",
          t);
    }
  }

  @RollbackOnError
  public void rollback() throws MojoExecutionException {
    this.log.info("Rollback of artifact-spy-plugin addition to the build configuration.");
    try {
      for (Entry<MavenProject, Document> entry : this.cachedPOMs.entrySet()) {
        PomUtil.writePOM(entry.getValue(), entry.getKey());
      }
    } catch (Throwable t) {
      throw new MojoExecutionException("Could not remove artifact-spy-plugin from the POM.", t);
    }
  }
}
