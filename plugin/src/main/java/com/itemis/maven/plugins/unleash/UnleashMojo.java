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

import com.itemis.maven.plugins.cdi.AbstractCDIMojo;
import com.itemis.maven.plugins.cdi.annotations.MojoProduces;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@Mojo(name = "perform", aggregator = true, requiresProject = true)
public class UnleashMojo extends AbstractCDIMojo implements Extension {
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

  @Parameter(property = "unleash.profiles")
  @MojoProduces
  @Named("profiles")
  private List<String> profiles;

  @Parameter(defaultValue = "${maven.home}", property = "unleash.mavenHome")
  @MojoProduces
  @Named("maven.home")
  private String mavenHome;

  @MojoProduces
  public MavenLogWrapper createLogWrapper() {
    MavenLogWrapper log = new MavenLogWrapper(getLog());
    if (this.enableLogTimestamps) {
      log.enableLogTimestamps();
    }
    return log;
  }
}
