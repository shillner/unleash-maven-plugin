package com.itemis.maven.plugins.unleash.steps.actions.tycho;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.requests.RevertCommitsRequest;
import com.itemis.maven.plugins.unleash.util.DevVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

/**
 * Uses Eclipse Tycho features to upgrade the POMs and MANIFESTs with the next development versions. It also updates
 * versions of bundle references.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.1.0
 */
@ProcessingStep(id = "setDevVersionTycho", description = "Uses the tycho-versions-plugin to update the POM and MANIFEST versions for the next development cycle.", requiresOnline = true)
public class SetDevVersionsTycho extends AbstractTychoVersionsStep implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("scmMessagePrefix")
  private String scmMessagePrefix;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;
  private ScmProvider scmProvider;
  @Inject
  private DevVersionUtil util;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Updating project modules with release versions (POM and MANIFEST versions)");
    super.execute(context);

    this.scmProvider = this.scmProviderRegistry.getProvider();
    for (MavenProject project : this.reactorProjects) {
      try {
        Optional<Document> parsedPOM = PomUtil.parsePOM(project);
        if (parsedPOM.isPresent()) {
          Document document = parsedPOM.get();
          this.util.revertScmSettings(project, document);
          PomUtil.writePOM(document, project);
        }
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for next development cycle.", t);
      }
    }

    this.util.commitChanges(false);
  }

  @Override
  protected ReleasePhase currentReleasePhase() {
    return ReleasePhase.POST_RELEASE;
  }

  @Override
  @RollbackOnError
  public void rollback() throws MojoExecutionException, MojoFailureException {
    this.log.info("Rollback of all version changes necessary for the next development cycle (POMs, MANIFESTs, ...).");

    StringBuilder message = new StringBuilder(
        "Reversion of failed release build (step: setting of next snapshot version).");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    RevertCommitsRequest revertCommitsRequest = RevertCommitsRequest.builder()
        .fromRevision(this.metadata.getScmRevisionAfterNextDevVersion())
        .toRevision(this.metadata.getScmRevisionBeforeNextDevVersion()).message(message.toString()).merge()
        .mergeClient(new ScmPomVersionsMergeClient()).push().build();
    this.scmProvider.revertCommits(revertCommitsRequest);

    // rolls back the version changes using tycho
    super.rollback();
  }
}
