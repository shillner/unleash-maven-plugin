package com.itemis.maven.plugins.unleash;

import java.util.List;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Named;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.itemis.maven.plugins.cdi.AbstractCdiMojo;
import com.itemis.maven.plugins.cdi.annotations.MojoProduces;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@Mojo(name = "perform", aggregator = true, requiresProject = true)
public class UnleashMojo extends AbstractCdiMojo implements Extension {
  @Component
  @MojoProduces
  public RepositorySystem repoSystem;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  @MojoProduces
  public RepositorySystemSession repoSession;

  @Parameter(readonly = true, defaultValue = "${project.remotePluginRepositories}")
  @MojoProduces
  @Named("pluginRepositories")
  public List<RemoteRepository> remotePluginRepos;

  @Parameter(readonly = true, defaultValue = "${project.remoteProjectRepositories}")
  @MojoProduces
  @Named("projectRepositories")
  public List<RemoteRepository> remoteProjectRepos;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  @MojoProduces
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Component
  @MojoProduces
  private MavenProject project;

  @Parameter
  @MojoProduces
  @Named("developmentVersion")
  private String developmentVersion;

  @Parameter
  @MojoProduces
  @Named("releaseVersion")
  private String releaseVersion;

  @Parameter(defaultValue = "true", property = "unleash.logTimestamps")
  @MojoProduces
  @Named("enableLogTimestamps")
  private boolean enableLogTimestamps;

  @Parameter(defaultValue = "true", property = "unleash.allowLocalReleaseArtifacts")
  @MojoProduces
  @Named("allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  @MojoProduces
  public MavenLogWrapper createLogWrapper() {
    MavenLogWrapper log = new MavenLogWrapper(getLog());
    if (this.enableLogTimestamps) {
      log.enableLogTimestamps();
    }
    return log;
  }

  // private List<UnleashProcessingAction> setupActions() {
  // this.logger.debug("Initializing the release actions.");
  // List<UnleashProcessingAction> actions = Lists.newArrayList();
  //
  // // QUESTION how can we describe the workflow and setup the actions in another, more flexible way?
  // // users should be able to include and exclude actions that are not needed!
  // CheckScmRevision checkScmRevision = new CheckScmRevision();
  // addCommonActionParams(checkScmRevision);
  // actions.add(checkScmRevision);
  //
  // CheckReleasable releasableCheckAction = new CheckReleasable();
  // addCommonActionParams(releasableCheckAction);
  // actions.add(releasableCheckAction);
  //
  // CheckAether checkAetherAction = new CheckAether(this.releaseVersion);
  // addCommonActionParams(checkAetherAction);
  // checkAetherAction.allowLocalReleaseArtifacts(this.allowLocalReleaseArtifacts);
  // actions.add(checkAetherAction);
  //
  // this.logger.debug("Release action order:");
  // for (UnleashProcessingAction action : actions) {
  // this.logger.debug("\t" + action.getClass().getSimpleName());
  // }
  // this.logger.debug("");
  // return actions;
  // }
  //
  // private void addCommonActionParams(AbstractProcessingAction action) {
  // action.setLog(this.logger);
  // action.setReactorProjects(this.reactorProjects);
  // action.setRemoteProjectRepos(this.remoteProjectRepos);
  // action.setRepoSession(this.repoSession);
  // action.setRepoSystem(this.repoSystem);
  // }
}
