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

import com.google.common.base.Optional;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest.Builder;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import com.itemis.maven.plugins.unleash.util.PomUtil;
import com.itemis.maven.plugins.unleash.util.functions.FileToRelativePath;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

@ProcessingStep(id = "setDevVersion", description = "Updates the projects with the next development versions", requiresOnline = true)
public class SetNextDevVersion implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

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
  public void execute() throws MojoExecutionException, MojoFailureException {
    init();

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

  private void init() {
    Optional<ScmProvider> provider = this.scmProviderRegistry.getProvider();
    if (!provider.isPresent()) {
      throw new IllegalStateException(
          "Could not load the SCM provider, please check previous log entries. Maybe you need to add an appropriate provider implementation as a dependency to the plugin.");
    }
    this.scmProvider = provider.get();
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
    StringBuilder message = new StringBuilder("Preparation for next development cycle.");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    MergeClient mergeClient = new ScmPomVersionsMergeClient();
    Builder requestBuilder = CommitRequest.builder().merge().mergeClient(mergeClient).message(message.toString())
        .push();
    FileToRelativePath pathConverter = new FileToRelativePath(this.project.getBasedir());
    for (MavenProject p : this.reactorProjects) {
      requestBuilder.addPaths(pathConverter.apply(p.getFile()));
    }

    this.scmProvider.commit(requestBuilder.build());
  }
}
