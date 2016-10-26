package com.itemis.maven.plugins.unleash.steps.actions.nexus;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.sonatype.nexus.maven.staging.StagingAction;
import org.sonatype.nexus.maven.staging.StagingActionMessages;
import org.sonatype.nexus.maven.staging.deploy.DeployableArtifact;
import org.sonatype.nexus.maven.staging.deploy.strategy.DeployPerModuleRequest;
import org.sonatype.nexus.maven.staging.deploy.strategy.DeployStrategy;
import org.sonatype.nexus.maven.staging.deploy.strategy.FinalizeDeployRequest;
import org.sonatype.nexus.maven.staging.remote.Parameters;

import com.google.common.collect.Lists;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.util.functions.AetherToMavenArtifact;

@ProcessingStep(id = "deploy-nexus-staging", description = "Deploys to Nexus with execution of the staging workflow.")
public class DeployArtifactsNexusStaging implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private DeployStrategy deployStrategy;
  @Inject
  private MavenSession session;
  @Inject
  private ReleaseMetadata metadata;
  @Inject
  private MavenProject project;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Deploying the release artifacts using Nexus staging workflow");

    Parameters parameters = buildParameters();
    List<DeployableArtifact> artifacts = Lists.newArrayList();
    for (Artifact a : this.metadata.getReleaseArtifacts()) {
      artifacts.add(new DeployableArtifact(a.getFile(), AetherToMavenArtifact.INSTANCE.apply(a)));
    }

    DeployPerModuleRequest request = new DeployPerModuleRequest(this.session, parameters, artifacts);
    try {
      this.deployStrategy.deployPerModule(request);
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    try {
      final FinalizeDeployRequest finalizeRequest = new FinalizeDeployRequest(this.session, parameters);
      finalizeRequest.setRemoteNexus(request.getRemoteNexus()); // pass over client
      this.deployStrategy.finalizeDeploy(finalizeRequest);
    } catch (ArtifactDeploymentException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private File getWorkDirectoryRoot() {
    return new File(this.session.getExecutionRootDirectory() + "/target/nexus-staging");
  }

  private StagingActionMessages getStagingActionMessages() throws MojoExecutionException {
    final HashMap<StagingAction, String> messages = new HashMap<StagingAction, String>();
    return new StagingActionMessages(null, messages,
        this.project.getGroupId() + ':' + this.project.getArtifactId() + ':' + this.project.getVersion());
  }

  private Parameters buildParameters() throws MojoExecutionException {
    try {
      final Parameters parameters = new Parameters("com.itemis.maven.plugins:unleash-maven-plugin:2.1.3",
          new File(getWorkDirectoryRoot(), "deferred"), new File(getWorkDirectoryRoot(), "staging"));
      parameters.setNexusUrl("http://192.168.99.100:8081/nexus/");
      parameters.setServerId("nexus-le");
      parameters.setKeepStagingRepositoryOnCloseRuleFailure(true);
      parameters.setAutoReleaseAfterClose(true);
      parameters.setAutoDropAfterRelease(true);
      parameters.setStagingActionMessages(getStagingActionMessages());
      parameters.setStagingProgressTimeoutMinutes(5);
      parameters.setStagingProgressPauseDurationSeconds(3);
      parameters.setSslInsecure(true);
      parameters.setSslAllowAll(true);
      parameters.setKeepStagingRepositoryOnFailure(true);
      parameters.setSkipStagingRepositoryClose(false);
      return parameters;
    } catch (Exception e) {
      throw new MojoExecutionException("Bad configuration:" + e.getMessage(), e);
    }
  }
}
