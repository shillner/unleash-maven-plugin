package com.itemis.maven.plugins.unleash.util;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest;
import com.itemis.maven.plugins.unleash.scm.requests.CommitRequest.Builder;
import com.itemis.maven.plugins.unleash.util.functions.FileToRelativePath;
import com.itemis.maven.plugins.unleash.util.scm.ScmPomVersionsMergeClient;
import com.itemis.maven.plugins.unleash.util.scm.ScmProviderRegistry;

/**
 * Provides some utility methods for the processing steps that update the projects with the new development versions.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.1.0
 */
public class DevVersionUtil {
  @Inject
  private Logger log;
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
  private ScmProvider scmProvider;

  @PostConstruct
  private void init() {
    this.scmProvider = this.scmProviderRegistry.getProvider();
  }

  public void revertScmSettings(MavenProject projectToRevert, Document document) {
    Scm scm = this.metadata.getCachedScmSettings(projectToRevert);
    if (scm != null) {
      this.log.debug("\t\tReversion of SCM connection tags");
      Document originalPOM = this.metadata.getCachedOriginalPOM(projectToRevert);

      Node scmNode = PomUtil.getOrCreateScmNode(document, false);
      Node originalScmNode = PomUtil.getOrCreateScmNode(originalPOM, false);

      if (scmNode != null) {
        Optional<String> connection = PomUtil.getChildNodeTextContent(originalScmNode,
            PomUtil.NODE_NAME_SCM_CONNECTION);
        if (connection.isPresent()) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_CONNECTION, connection.get(), false);
        }

        Optional<String> devConnection = PomUtil.getChildNodeTextContent(originalScmNode,
            PomUtil.NODE_NAME_SCM_DEV_CONNECTION);
        if (devConnection.isPresent()) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_DEV_CONNECTION, devConnection.get(), false);
        }

        Optional<String> url = PomUtil.getChildNodeTextContent(originalScmNode, PomUtil.NODE_NAME_SCM_URL);
        if (url.isPresent()) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_URL, url.get(), false);
        }

        if (scm.getTag() != null) {
          PomUtil.setNodeTextContent(scmNode, PomUtil.NODE_NAME_SCM_TAG, scm.getTag(), false);
        } else {
          PomUtil.deleteNode(scmNode, PomUtil.NODE_NAME_SCM_TAG);
        }
      }
    }
  }

  public void commitChanges(boolean commitPomsOnly) {
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
    if (commitPomsOnly) {
      for (MavenProject p : this.reactorProjects) {
        requestBuilder.addPaths(pathConverter.apply(p.getFile()));
      }
    }

    String newRevision = this.scmProvider.commit(requestBuilder.build());
    this.metadata.setScmRevisionAfterNextDevVersion(newRevision);
  }
}
