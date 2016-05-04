package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

@ProcessingStep(@Goal(name = "perform", stepNumber = 0))
public class InitMetadataAndRevertPomChanges implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  private ReleaseMetadata metadata;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // nothing to do here since this is only a step for the reversal of all pom modifications
    // Metadata initialization is done implicitly as soon as the metadata instance is created, which is the cause when
    // injecting it here
  }

  @RollbackOnError
  public void rollback() {
    this.log.info("Rolling back all POM modifiactions due to a processing exception.");
    for (MavenProject p : this.reactorProjects) {
      this.log.debug("Rolling back POM modifications of project: " + ProjectToString.INSTANCE.apply(p));
      PomUtil.writePOM(this.metadata.getCachedDocument(p), p);
    }
  }
}
