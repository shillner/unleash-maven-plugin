package com.itemis.maven.plugins.unleash.steps.actions;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.requests.RevertCommitsRequest;
import com.itemis.maven.plugins.unleash.util.DevVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.VersionUpgradeStrategy;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToCoordinates;
import com.itemis.maven.plugins.unleash.util.functions.ProjectToString;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

/**
 * Updates the POMs of all modules of the project with the new development version, reverts all SCM changes so that the
 * SCM paths match the current branch again and commits the changes finally.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "setDevVersion", description = "Updates the projects with the next development versions, reverts previous SCM path changes and finally commits the changes to the current branch.", requiresOnline = true)
public class SetNextDevVersion extends AbstractVersionsStep {
  @Inject
  MavenProject project;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;
  @Inject
  @Named("scmMessagePrefix")
  private String scmMessagePrefix;
  @Inject
  private DevVersionUtil util;
  @Inject
  private VersionUpgradeStrategy versionUpgradeStrategy;
  private ScmProvider scmProvider;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Preparing project modules for next development cycle.");

    this.scmProvider = this.scmProviderRegistry.getProvider();
    this.cachedPOMs = Maps.newHashMap();

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tPreparing module '" + ProjectToString.INSTANCE.apply(project) + "'.");
      this.cachedPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(project), PomUtil.parsePOM(project).get());

      try {
        Document document = loadAndProcess(project);
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for next development cycle.", t);
      }
    }

    this.util.commitChanges(true);
  }

  @Override
  protected Document loadAndProcess(MavenProject project) {
    Document document = super.loadAndProcess(project);
    this.util.revertScmSettings(project, document);
    return document;
  }

  @Override
  protected void logProjectVersionUpdate(MavenProject project, String oldVersion, String newVersion) {
    if (this.log.isDebugEnabled()) {
      this.log.debug("\t\tUpdate of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' ["
          + oldVersion + " => " + newVersion + "] Version Upgrade Strategy: " + this.versionUpgradeStrategy.name());
    }
  }

  @Override
  protected void logParentVersionUpdate(MavenProject project, ArtifactCoordinates oldCoordinates,
      ArtifactCoordinates newCoordinates) {
    if (this.log.isDebugEnabled()) {
      this.log.debug("\t\tUpdate of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
          + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
    }
  }

  @Override
  protected ReleasePhase previousReleasePhase() {
    return ReleasePhase.RELEASE;
  }

  @Override
  protected ReleasePhase currentReleasePhase() {
    return ReleasePhase.POST_RELEASE;
  }

  @RollbackOnError
  public void rollback() throws MojoExecutionException {
    this.log.info(
        "Rollback of all pom changes necessary for setting of the development version as well as reverting any made SCM commits.");

    StringBuilder message = new StringBuilder(
        "Reversion of failed release build (step: setting of next snapshot version).");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    if (this.metadata.getScmRevisionAfterNextDevVersion() != null) {
      // #49 (https://github.com/shillner/unleash-maven-plugin/issues/49)
      // commit reversion is only performed if the commit didn't fail (revision after dev version setting is not null)
      RevertCommitsRequest revertCommitsRequest = RevertCommitsRequest.builder()
          .fromRevision(this.metadata.getScmRevisionAfterNextDevVersion())
          .toRevision(this.metadata.getScmRevisionBeforeNextDevVersion()).message(message.toString()).merge()
          .mergeClient(new ScmPomVersionsMergeClient()).push().build();
      this.scmProvider.revertCommits(revertCommitsRequest);
    }

    for (MavenProject project : this.reactorProjects) {
      Document document = this.cachedPOMs.get(ProjectToCoordinates.EMPTY_VERSION.apply(project));
      if (document != null) {
        try {
          PomUtil.writePOM(document, this.project);
        } catch (Throwable t) {
          throw new MojoExecutionException(
              "Could not revert the setting of development versions after a failed release build.", t);
        }
      }
    }
  }
}
