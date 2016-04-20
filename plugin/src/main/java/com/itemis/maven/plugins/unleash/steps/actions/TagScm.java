package com.itemis.maven.plugins.unleash.steps.actions;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.Goal;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.ReleasePhase;
import com.itemis.maven.plugins.unleash.scm.ScmException;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.ScmProviderRegistry;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;

@ProcessingStep(@Goal(name = "perform", stepNumber = 70))
public class TagScm implements CDIMojoProcessingStep {
  @Inject
  private MavenProject project;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  @Named("tagNamePattern")
  private String tagNamePattern;
  @Inject
  @Named("scmMessagePrefix")
  private String scmMessagePrefix;
  @Inject
  private ScmProviderRegistry scmProviderRegistry;

  private ScmProvider scmProvider;
  private String globalReleaseVersion;

  private void init() {
    Optional<ScmProvider> provider = this.scmProviderRegistry.getProvider();
    if (!provider.isPresent()) {
      throw new IllegalStateException(
          "Could not load the SCM provider, please check previous log entries. Maybe you need to add an appropriate provider implementation as a dependency to the plugin.");
    }
    this.scmProvider = provider.get();

    this.metadata.setScmTagName(ReleaseUtil.getTagName(this.tagNamePattern, this.project));

    Map<ReleasePhase, ArtifactCoordinates> coordinates = this.metadata
        .getArtifactCoordinatesByPhase(this.project.getGroupId(), this.project.getArtifactId());
    ArtifactCoordinates postReleaseCoordinates = coordinates.get(ReleasePhase.POST);
    this.globalReleaseVersion = postReleaseCoordinates.getVersion();
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    init();

    StringBuilder message = new StringBuilder("Tag for release version ").append(this.globalReleaseVersion)
        .append(" (base revision: ").append(this.metadata.getScmRevision(ReleasePhase.PRE)).append(")");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    this.scmProvider.tag(this.metadata.getScmTagName(), this.metadata.getScmRevision(ReleasePhase.PRE),
        message.toString());
  }

  @RollbackOnError(ScmException.class)
  private void deleteTag(ScmException e) {
    this.scmProvider.deleteTag(this.metadata.getScmTagName());
  }
}
