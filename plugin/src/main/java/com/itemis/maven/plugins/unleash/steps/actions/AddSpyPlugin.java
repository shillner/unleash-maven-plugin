package com.itemis.maven.plugins.unleash.steps.actions;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;

@ProcessingStep(id = "addSpyPlugin", description = "Adds the artifact-spy-plugin to the build configuration. This plugin determines the built artifacts for later installation and deployment.")
public class AddSpyPlugin implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;
  @Inject
  private MavenProject project;
  @Inject
  @Named("artifactSpyPlugin")
  private ArtifactCoordinates artifactSpyPluginCoordinates;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.log.info(
        "Adding artifact-spy-plugin to the build configuration. This plugin is required to determine the artifacts that are produced by the build for later installation and deployment.");
    try {
      Document document = PomUtil.parsePOM(this.project);

      Node plugin = PomUtil.createPlugin(document, this.artifactSpyPluginCoordinates.getGroupId(),
          this.artifactSpyPluginCoordinates.getArtifactId(), this.artifactSpyPluginCoordinates.getVersion());
      PomUtil.createPluginExecution(plugin, "spy", "verify", "spy");

      PomUtil.writePOM(document, this.project);
    } catch (Throwable t) {
      throw new MojoFailureException(
          "Could not add the artifact-spy-plugin to the POM. This plugin is required to determine the artifacts that are produced by the build for later installation and deployment.",
          t);
    }
  }

  @RollbackOnError
  public void rollback() {
    this.log.info("Rolling back POM plugin modifiaction due to a processing exception.");
    try {
      Document document = PomUtil.parsePOM(this.project);

      this.log.debug("Removing artifact-spy-plugin from build configuration.");
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

        PomUtil.writePOM(document, this.project);
      }
    } catch (Throwable t) {
      throw new RuntimeException("Could not remove the artifact-spy-plugin from the POM.", t);
    }
  }
}
