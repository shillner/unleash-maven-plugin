package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Objects;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.requests.DeleteTagRequest;
import com.itemis.maven.plugins.unleash.scm.requests.RevertCommitsRequest;
import com.itemis.maven.plugins.unleash.scm.requests.TagRequest;
import com.itemis.maven.plugins.unleash.scm.requests.TagRequest.Builder;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

@ProcessingStep(id = "tagScm", description = "Creates an SCM Tag with the release setup.", requiresOnline = true)
public class TagScm implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;
  @Inject
  private MavenProject project;
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
  @Inject
  @Named("commitBeforeTagging")
  private boolean commitBeforeTagging;

  private ScmProvider scmProvider;
  private String globalReleaseVersion;
  private boolean tagWasPresent;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    init();

    String scmTagName = this.metadata.getScmTagName();
    updateScmConnections(scmTagName);

    this.log.info("Tagging SCM, tag name: " + scmTagName);
    if (this.scmProvider.hasTag(scmTagName)) {
      this.tagWasPresent = true;
      throw new MojoFailureException("A tag with name " + scmTagName + " already exists.");
    }

    StringBuilder message = new StringBuilder("Tag for release version ").append(this.globalReleaseVersion)
        .append(" (base revision: ").append(this.metadata.getInitialScmRevision()).append(")");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    Builder requestBuilder = TagRequest.builder().message(message.toString()).tagName(scmTagName).push();
    if (this.commitBeforeTagging) {
      this.metadata.setScmRevisionBeforeTag(this.scmProvider.getLatestRemoteRevision());
      String remoteRevision = this.scmProvider.getLatestRemoteRevision();
      if (!Objects.equal(remoteRevision, this.metadata.getInitialScmRevision())) {
        throw new MojoFailureException("Error creating the SCM tag! "
            + "A commit before tag creation was requested but the remote repository changed since we started the release. "
            + "Creating the tag with first committing the local changes would result in an invalid tag!");
      }

      requestBuilder.commitBeforeTagging();
      StringBuilder preTagMessage = new StringBuilder("Preparation for tag ").append(scmTagName);
      if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
        preTagMessage.insert(0, this.scmMessagePrefix);
      }
      requestBuilder.preTagCommitMessage(preTagMessage.toString());
    }

    String newRevision = this.scmProvider.tag(requestBuilder.build());
    this.metadata.setScmRevisionAfterTag(newRevision);
  }

  private void init() {
    this.scmProvider = this.scmProviderRegistry.getProvider();
    Map<ReleasePhase, ArtifactCoordinates> coordinates = this.metadata
        .getArtifactCoordinatesByPhase(this.project.getGroupId(), this.project.getArtifactId());
    ArtifactCoordinates postReleaseCoordinates = coordinates.get(ReleasePhase.RELEASE);
    this.globalReleaseVersion = postReleaseCoordinates.getVersion();
  }

  private void updateScmConnections(String scmTagName) throws MojoFailureException {
    for (MavenProject p : this.reactorProjects) {
      Scm scm = p.getModel().getScm();
      if (scm != null) {
        try {
          Document document = PomUtil.parsePOM(p);
          Node scmNode = PomUtil.getOrCreateScmNode(document, false);

          if (scmNode != null) {
            if (scm.getConnection() != null) {
              PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_CONNECTION,
                  this.scmProvider.calculateTagConnectionString(scm.getConnection(), scmTagName), false);
            }

            if (scm.getDeveloperConnection() != null) {
              PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_DEV_CONNECTION,
                  this.scmProvider.calculateTagConnectionString(scm.getDeveloperConnection(), scmTagName), false);
            }

            if (!this.scmProvider.isTagInfoIncludedInConnection()) {
              PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_TAG, scmTagName, true);
            }
            PomUtil.writePOM(document, p);
          }
        } catch (Throwable t) {
          throw new MojoFailureException("Could not update scm information for release.", t);
        }
      }
    }
  }

  @RollbackOnError
  private void rollback() {
    this.log.info("Rolling back SCM tagging due to a processing exception.");
    String scmTagName = this.metadata.getScmTagName();
    if (this.scmProvider.hasTag(scmTagName) && !this.tagWasPresent) {
      this.log.debug("Deleting scm tag '" + scmTagName + "' since the release build failed.");

      StringBuilder message = new StringBuilder("Deletion of tag '").append(scmTagName)
          .append("' due to release rollback.");
      if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
        message.insert(0, this.scmMessagePrefix);
      }

      DeleteTagRequest request = DeleteTagRequest.builder().message(message.toString()).tagName(scmTagName).push()
          .build();
      this.scmProvider.deleteTag(request);
    } else {
      this.log.debug("Skipping deletion of SCM tag '" + scmTagName
          + "' since the tag was already present before the release build was triggered.");
    }

    if (this.commitBeforeTagging) {
      StringBuilder message = new StringBuilder("Reversion of failed release build (step: tag SCM).");
      if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
        message.insert(0, this.scmMessagePrefix);
      }
      RevertCommitsRequest revertCommitsRequest = RevertCommitsRequest.builder()
          .fromRevision(this.metadata.getScmRevisionAfterTag()).toRevision(this.metadata.getScmRevisionBeforeTag())
          .message(message.toString()).merge().mergeClient(new ScmPomVersionsMergeClient()).build();
      this.scmProvider.revertCommits(revertCommitsRequest);
    }
  }
}
