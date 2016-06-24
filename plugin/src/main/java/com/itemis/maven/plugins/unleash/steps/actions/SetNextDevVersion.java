package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest.Builder;
import com.itemis.maven.plugins.unleash.scm.requests.RevertCommitsRequest;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.FileToRelativePath;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

@ProcessingStep(id = "setDevVersion", description = "Updates the projects with the next development versions", requiresOnline = true)
public class SetNextDevVersion implements CDIMojoProcessingStep {
  @Inject
  private Logger log;

  @Inject
  private ReleaseMetadata metadata;

  @Inject
  MavenProject project;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  private ScmProviderRegistry scmProviderRegistry;

  @Inject
  @Named("scmMessagePrefix")
  private String scmMessagePrefix;

  private ScmProvider scmProvider;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.scmProvider = this.scmProviderRegistry.getProvider();

    for (MavenProject project : this.reactorProjects) {
      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document);
        setParentVersion(project, document);
        revertScmSettings(project, document);
        PomUtil.writePOM(document, project);
        commitChanges();
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for next development cycle.", t);
      }
    }
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVerion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();
    String newVersion = coordinatesByPhase.get(ReleasePhase.POST_RELEASE).getVersion();
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
    this.log.info("Update of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' [" + oldVerion
        + " => " + newVersion + "]");
  }

  private void setParentVersion(MavenProject project, Document document) {
    Parent parent = project.getModel().getParent();
    if (parent != null) {
      Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
          .getArtifactCoordinatesByPhase(parent.getGroupId(), parent.getArtifactId());
      ArtifactCoordinates oldCoordinates = coordinatesByPhase.get(ReleasePhase.RELEASE);
      ArtifactCoordinates newCoordinates = coordinatesByPhase.get(ReleasePhase.POST_RELEASE);

      // null indicates that the parent is not part of the reactor projects since no release version had been calculated
      // for it
      if (newCoordinates != null) {
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
        this.log.info("Update of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
      }
    }
  }

  private void revertScmSettings(MavenProject project, Document document) {
    Scm scm = this.metadata.getCachedScmSettings(project.getGroupId() + ":" + project.getArtifactId());
    if (scm != null) {
      Node scmNode = PomUtil.getOrCreateScmNode(document, false);

      if (scmNode != null) {
        if (scm.getConnection() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_CONNECTION, scm.getConnection(), false);
        }

        if (scm.getDeveloperConnection() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_DEV_CONNECTION, scm.getDeveloperConnection(),
              false);
        }

        if (scm.getTag() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_TAG, scm.getTag(), false);
        } else {
          PomUtil.deleteNode(scmNode, PomUtil.NODE_NAME_SCM_TAG);
        }
      }
    }
  }

  private void commitChanges() {
    this.metadata.setScmRevisionBeforeNextDevVersion(this.scmProvider.getLatestRemoteRevision());

    StringBuilder message = new StringBuilder("Preparation for next development cycle.");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    Builder requestBuilder = CommitRequest.builder().merge().mergeClient(new ScmPomVersionsMergeClient())
        .message(message.toString()).push();
    FileToRelativePath pathConverter = new FileToRelativePath(this.project.getBasedir());
    for (MavenProject p : this.reactorProjects) {
      requestBuilder.addPaths(pathConverter.apply(p.getFile()));
    }

    String newRevision = this.scmProvider.commit(requestBuilder.build());
    this.metadata.setScmRevisionAfterNextDevVersion(newRevision);
  }

  @RollbackOnError
  public void rollback() {
    StringBuilder message = new StringBuilder(
        "Reversion of failed release build (step: setting of next snapshot version).");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    RevertCommitsRequest revertCommitsRequest = RevertCommitsRequest.builder()
        .fromRevision(this.metadata.getScmRevisionAfterNextDevVersion())
        .toRevision(this.metadata.getScmRevisionBeforeNextDevVersion()).message(message.toString()).merge()
        .mergeClient(new ScmPomVersionsMergeClient()).build();
    this.scmProvider.revertCommits(revertCommitsRequest);
  }
}
