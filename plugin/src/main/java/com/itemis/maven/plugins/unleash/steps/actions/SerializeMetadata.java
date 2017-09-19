package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;

/**
 * Serializes the release metadata into a properties file located in the output directory of the project.<br/>
 * In case of multimodule projects the output file will be serialized to the output folder of the reactor project.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.7.0
 */
@ProcessingStep(id = "serializeMetadata", description = "serializes the release metadata to a properties file into the output folder of the project.", requiresOnline = false)
public class SerializeMetadata implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private MavenProject project;
  @Inject
  private ReleaseMetadata metadata;
  private File metadataOutputFile;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.metadataOutputFile = new File(this.project.getBuild().getDirectory(), "releaseMetadata.properties");
    this.log.info("Serializing the release metadata into file '" + this.metadataOutputFile.getAbsolutePath() + "'.");

    try (FileOutputStream os = new FileOutputStream(this.metadataOutputFile)) {
      this.metadata.toProperties().store(os, "The unleash release metadata");
    } catch (IOException e) {
      throw new MojoExecutionException("An error occurred during the serialization of the release metadata into file '"
          + this.metadataOutputFile.getAbsolutePath() + "'.");
    }
  }

  @RollbackOnError
  public void rollback() {
    this.log.info("Rolling back release metadata serialization due to a processing exception.");
    if (this.metadataOutputFile.exists()) {
      this.log.debug("\tDeleting metadata output file '" + this.metadataOutputFile.getAbsolutePath() + "'");
      this.metadataOutputFile.delete();
    }
  }
}
