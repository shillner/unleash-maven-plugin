package de.itemis.maven.plugins.unleash.actions;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.google.common.base.Preconditions;

import de.itemis.maven.aether.ArtifactResolver;
import de.itemis.maven.plugins.unleash.UnleashProcessingAction;
import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

public abstract class AbstractProcessingAction implements UnleashProcessingAction {
  private List<MavenProject> reactorProjects;
  private RepositorySystem repoSystem;
  private RepositorySystemSession repoSession;
  private List<RemoteRepository> remoteProjectRepos;
  private MavenLogWrapper log;
  private ArtifactResolver artifactResolver;

  public void setRepoSystem(RepositorySystem repoSystem) {
    this.repoSystem = repoSystem;
  }

  public void setRepoSession(RepositorySystemSession repoSession) {
    this.repoSession = repoSession;
  }

  public void setRemoteProjectRepos(List<RemoteRepository> remoteProjectRepos) {
    this.remoteProjectRepos = remoteProjectRepos;
  }

  public void setReactorProjects(List<MavenProject> reactorProjects) {
    this.reactorProjects = reactorProjects;
  }

  protected final List<MavenProject> getReactorProjects() {
    return this.reactorProjects;
  }

  public void setLog(MavenLogWrapper log) {
    this.log = log;
  }

  protected final MavenLogWrapper getLog() {
    return this.log;
  }

  protected final ArtifactResolver getArtifactResolver() {
    return this.artifactResolver;
  }

  @Override
  public void prepare() {
    validate();
    this.artifactResolver = new ArtifactResolver(this.repoSystem, this.repoSession, this.remoteProjectRepos, this.log);
    doPrepare();
  }

  /**
   * To be implemented by extending classes that need an additional preparation step prior to the action execution.
   * <br>
   * This method will be invoked automatically prior to the action execution but after the validation step.
   */
  protected void doPrepare() {
  }

  private void validate() {
    Preconditions.checkState(this.log != null, "A logger is needed for action reporting.");
    this.log.setContextClass(getClass());

    Preconditions.checkState(this.reactorProjects != null && !this.reactorProjects.isEmpty(),
        "There are no reactor projects set!");
    Preconditions.checkState(this.repoSystem != null, "The repository system needs to be set for artifact resolving.");
    Preconditions.checkState(this.repoSession != null, "A repository session is required for artifact resolving.");
    Preconditions.checkState(this.remoteProjectRepos != null && !this.remoteProjectRepos.isEmpty(),
        "No remote repositories configured for artifact resolving.");
    doValidate();
  }

  /**
   * To be implemented by extending classes that need validation of the action settings before the action is executed.
   * <br>
   * This method will be invoked automatically prior to the action execution.
   */
  protected void doValidate() {
  }

  /**
   * The default implementation to avoid overriding this method again and again.
   */
  @Override
  public void rollback() {
  }
}
