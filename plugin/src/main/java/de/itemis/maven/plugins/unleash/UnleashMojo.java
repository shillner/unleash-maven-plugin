package de.itemis.maven.plugins.unleash;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.google.common.collect.Lists;

import de.itemis.maven.plugins.unleash.actions.AbstractProcessingAction;
import de.itemis.maven.plugins.unleash.actions.CheckAether;
import de.itemis.maven.plugins.unleash.actions.CheckReleasable;
import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@Mojo(name = "perform", aggregator = true, requiresProject = true)
public class UnleashMojo extends AbstractMojo {
  @Component
  public RepositorySystem repoSystem;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  public RepositorySystemSession repoSession;

  @Parameter(readonly = true, defaultValue = "${project.remotePluginRepositories}")
  public List<RemoteRepository> remotePluginRepos;

  @Parameter(readonly = true, defaultValue = "${project.remoteProjectRepositories}")
  public List<RemoteRepository> remoteProjectRepos;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  private List<MavenProject> reactorProjects;

  @Parameter
  private String developmentVersion;

  @Parameter
  private String releaseVersion;

  @Parameter(defaultValue = "false", property = "unleash.logTimestamps")
  private boolean enableLogTimestamps;

  @Parameter(defaultValue = "true", property = "unleash.allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  private MavenLogWrapper logger;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    init();

    List<UnleashProcessingAction> actions = setupActions();
    for (UnleashProcessingAction action : actions) {
      action.prepare();
      action.execute();
    }
  }

  private void init() {
    this.logger = new MavenLogWrapper(getLog());
    if (this.enableLogTimestamps) {
      this.logger.enableLogTimestamps();
    }
  }

  private List<UnleashProcessingAction> setupActions() {
    this.logger.debug("Initializing the release actions.");
    List<UnleashProcessingAction> actions = Lists.newArrayList();

    CheckReleasable releasableCheckAction = new CheckReleasable();
    addCommonActionParams(releasableCheckAction);
    actions.add(releasableCheckAction);

    CheckAether checkAetherAction = new CheckAether(this.releaseVersion);
    addCommonActionParams(checkAetherAction);
    checkAetherAction.allowLocalReleaseArtifacts(this.allowLocalReleaseArtifacts);
    actions.add(checkAetherAction);

    this.logger.debug("Release action order:");
    for (UnleashProcessingAction action : actions) {
      this.logger.debug("\t" + action.getClass().getSimpleName());
    }
    this.logger.debug("");
    return actions;
  }

  private void addCommonActionParams(AbstractProcessingAction action) {
    action.setLog(this.logger);
    action.setReactorProjects(this.reactorProjects);
    action.setRemoteProjectRepos(this.remoteProjectRepos);
    action.setRepoSession(this.repoSession);
    action.setRepoSystem(this.repoSystem);
  }
}
