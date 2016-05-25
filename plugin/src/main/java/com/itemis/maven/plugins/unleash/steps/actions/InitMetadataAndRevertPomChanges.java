package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;

@ProcessingStep(id = "initMetadataAndRollbackPomChanges", description = "Initializes the release metadata and rolls back all POM changes in case of an error.", requiresOnline = false)
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
    for (MavenProject p : this.reactorProjects) {
      this.metadata.cacheScmSettings(p.getGroupId() + ":" + p.getArtifactId(), p.getModel().getScm());
    }
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
