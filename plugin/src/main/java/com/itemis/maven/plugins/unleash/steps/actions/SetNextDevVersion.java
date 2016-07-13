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

import com.google.common.collect.Maps;
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
  private Map<ArtifactCoordinates, Document> cachedPOMs;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Preparing project modules for next development cycle.");

    this.scmProvider = this.scmProviderRegistry.getProvider();
    this.cachedPOMs = Maps.newHashMap();

    for (MavenProject project : this.reactorProjects) {
      this.log.debug("\tPreparing module '" + ProjectToString.INSTANCE.apply(project) + "'.");
      this.cachedPOMs.put(ProjectToCoordinates.EMPTY_VERSION.apply(project), PomUtil.parsePOM(project));

      try {
        Document document = PomUtil.parsePOM(project);
        setProjectVersion(project, document);
        setParentVersion(project, document);
        revertScmSettings(project, document);
        PomUtil.writePOM(document, project);
      } catch (Throwable t) {
        throw new MojoFailureException("Could not update versions for next development cycle.", t);
      }
    }

    commitChanges();
  }

  private void setProjectVersion(MavenProject project, Document document) {
    Map<ReleasePhase, ArtifactCoordinates> coordinatesByPhase = this.metadata
        .getArtifactCoordinatesByPhase(project.getGroupId(), project.getArtifactId());
    String oldVerion = coordinatesByPhase.get(ReleasePhase.RELEASE).getVersion();
    String newVersion = coordinatesByPhase.get(ReleasePhase.POST_RELEASE).getVersion();
    this.log.debug("\t\tUpdate of module version '" + project.getGroupId() + ":" + project.getArtifact() + "' ["
        + oldVerion + " => " + newVersion + "]");
    PomUtil.setProjectVersion(project.getModel(), document, newVersion);
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
        this.log.debug("\t\tUpdate of parent version of module '" + project.getGroupId() + ":" + project.getArtifact()
            + "' [" + oldCoordinates.getVersion() + " => " + newCoordinates.getVersion() + "]");
        PomUtil.setParentVersion(project.getModel(), document, newCoordinates.getVersion());
      }
    }
  }

  private void revertScmSettings(MavenProject project, Document document) {
    Scm scm = this.metadata.getCachedScmSettings(project);
    if (scm != null) {
      this.log.debug("\t\tReversion of SCM connection tags");
      Node scmNode = PomUtil.getOrCreateScmNode(document, false);

      if (scmNode != null) {
        if (scm.getConnection() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_CONNECTION, scm.getConnection(), false);
        }

        if (scm.getDeveloperConnection() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_DEV_CONNECTION, scm.getDeveloperConnection(),
              false);
        }

        if (scm.getUrl() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_URL, scm.getUrl(), false);
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
    this.log.debug(
        "\tCommitting changed POMs of all modules and pushing to remote repository. Merging with remote changes if necessary.");
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
  public void rollback() throws MojoExecutionException {
    this.log.info(
        "Rollback of all pom changes necessary for setting of the development version as well as reverting any made SCM commits.");

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
