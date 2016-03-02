package de.itemis.maven.plugins.unleash;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.weld.environment.se.events.ContainerInitialized;

import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;

public class UnleashProcessor {
  @Inject
  public RepositorySystem repoSystem;

  @Inject
  public RepositorySystemSession repoSession;

  @Inject
  @Named("pluginRepositories")
  public List<RemoteRepository> remotePluginRepos;

  @Inject
  @Named("projectRepositories")
  public List<RemoteRepository> remoteProjectRepos;

  @Inject
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Inject
  @Named("developmentVersion")
  private String developmentVersion;

  @Inject
  @Named("releaseVersion")
  private String releaseVersion;

  @Inject
  @Named("enableLogTimestamps")
  private boolean enableLogTimestamps;

  @Inject
  @Named("allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  @Inject
  private MavenLogWrapper log;

  @Produces
  @Named("x")
  private String x = "x";

  public void process(@Observes ContainerInitialized event) {
    System.out.println("system: " + this.repoSystem);
    System.out.println("session: " + this.repoSession);
    System.out.println("dev: " + this.developmentVersion);
    System.out.println("rel: " + this.releaseVersion);
    System.out.println("ts: " + this.enableLogTimestamps);
    System.out.println("pluginRepos: " + this.remotePluginRepos);
    System.out.println("projectRepos: " + this.remoteProjectRepos);
    System.out.println("reactorProjects: " + this.reactorProjects);
    System.out.println("allowLocal: " + this.allowLocalReleaseArtifacts);
    System.out.println("log: " + this.log);
  }
}
