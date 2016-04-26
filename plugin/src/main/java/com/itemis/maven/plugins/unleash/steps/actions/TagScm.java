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
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.ScmProviderRegistry;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;
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
  @Inject
  private MavenLogWrapper log;

  private ScmProvider scmProvider;
  private String globalReleaseVersion;
  private boolean tagWasPresent;

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

    String scmTagName = this.metadata.getScmTagName();
    this.log.info("Tagging SCM, tag name: " + scmTagName);
    if (this.scmProvider.hasTag(scmTagName)) {
      this.tagWasPresent = true;
      throw new MojoFailureException("A tag with name " + scmTagName + " already exists.");
    }

    StringBuilder message = new StringBuilder("Tag for release version ").append(this.globalReleaseVersion)
        .append(" (base revision: ").append(this.metadata.getScmRevision(ReleasePhase.PRE)).append(")");
    if (StringUtils.isNotBlank(this.scmMessagePrefix)) {
      message.insert(0, this.scmMessagePrefix);
    }

    this.scmProvider.tag(scmTagName, this.metadata.getScmRevision(ReleasePhase.PRE), message.toString());
    throw new RuntimeException();
  }

  @RollbackOnError
  private void deleteTag() {
    this.log.info("Rolling back SCM tagging due to a processing exception.");
    String scmTagName = this.metadata.getScmTagName();
    if (this.scmProvider.hasTag(scmTagName) && !this.tagWasPresent) {
      this.log.debug("Deleting scm tag '" + scmTagName + "' since the release build failed.");
      this.scmProvider.deleteTag(scmTagName);
    } else {
      this.log.debug("Skipping deletion of SCM tag '" + scmTagName
          + "' since the tag was already present before the release build was triggered.");
    }
  }
}
