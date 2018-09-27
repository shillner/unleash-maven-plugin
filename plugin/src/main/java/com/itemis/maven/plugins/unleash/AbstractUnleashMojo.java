package com.itemis.maven.plugins.unleash;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.logging.Log;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.itemis.maven.aether.ArtifactCoordinates;
import com.itemis.maven.plugins.cdi.AbstractCDIMojo;
import com.itemis.maven.plugins.cdi.annotations.MojoInject;
import com.itemis.maven.plugins.cdi.annotations.MojoProduces;
import com.itemis.maven.plugins.unleash.util.Repository;
import com.itemis.maven.plugins.unleash.util.VersionUpgradeStrategy;

public class AbstractUnleashMojo extends AbstractCDIMojo {
  @Component
  @MojoProduces
  private PlexusContainer plexus;

  @Component
  @MojoProduces
  private RepositorySystem repoSystem;

  @Component
  @MojoProduces
  private RemoteRepositoryManager remoteRepositoryManager;

  @Component
  @MojoProduces
  private Deployer deployer;

  @Component
  @MojoProduces
  private Installer installer;

  @Component
  @MojoProduces
  private Prompter prompter;

  @Parameter(property = "session", readonly = true)
  @MojoProduces
  private MavenSession session;

  @Parameter(property = "mojoExecution", readonly = true)
  @MojoProduces
  private MojoExecution mojoExecution;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
  @MojoProduces
  private RepositorySystemSession repoSession;

  @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
  @MojoProduces
  @Named("pluginRepositories")
  private List<RemoteRepository> remotePluginRepos;

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
  @MojoProduces
  @Named("projectRepositories")
  private List<RemoteRepository> remoteProjectRepos;

  @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
  @MojoProduces
  @Named("local")
  private ArtifactRepository LocalRepository;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  @MojoProduces
  private MavenProject project;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  @MojoProduces
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Parameter(defaultValue = "${settings}", readonly = true, required = true)
  @MojoProduces
  private Settings settings;

  ////////////////////////////// configuration parameters //////////////////////////////
  ////////////////////////////// required
  @Parameter(defaultValue = "true", property = "unleash.allowLocalReleaseArtifacts", required = true)
  @MojoProduces
  @Named("allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  @Parameter(defaultValue = "false", property = "unleash.commitBeforeTagging", required = true)
  @MojoProduces
  @Named("commitBeforeTagging")
  private boolean commitBeforeTagging;

  @Parameter(defaultValue = "${maven.home}", property = "unleash.mavenHome", required = true)
  @MojoProduces
  @Named("maven.home")
  private String mavenHome;

  @Parameter(defaultValue = "@{project.version}", property = "unleash.tagNamePattern", required = true)
  @MojoProduces
  @Named("tagNamePattern")
  private String tagNamePattern;

  @Parameter(defaultValue = "true", property = "unleash.updateReactorDependencyVersion", required = true)
  @MojoProduces
  @Named("updateReactorDependencyVersion")
  private boolean updateReactorDependencyVersion;

  //////////////////////////// optional
  @Parameter(property = "unleash.developmentVersion", required = false)
  @MojoProduces
  @Named("developmentVersion")
  private String developmentVersion;

  @Parameter(defaultValue = "clean,verify", property = "unleash.goals", required = false)
  @MojoProduces
  @Named("releaseGoals")
  private List<String> goals;

  @Parameter(property = "unleash.profiles", required = false)
  @MojoProduces
  @Named("profiles")
  private List<String> profiles;

  @Parameter(defaultValue = "", property = "unleash.releaseArgs", required = false)
  private List<String> releaseArgs;

  @Parameter(property = "unleash.releaseVersion", required = false)
  @MojoProduces
  @Named("releaseVersion")
  private String releaseVersion;

  @Parameter(defaultValue = "[unleash-maven-plugin]", property = "unleash.scmMessagePrefix", required = false)
  private String scmMessagePrefix;

  @MojoProduces
  @Named("scmPassword")
  @Parameter(property = "unleash.scmPassword", required = false)
  private String scmPassword;

  @MojoProduces
  @Named("scmUsername")
  @Parameter(property = "unleash.scmUsername", required = false)
  private String scmUsername;

  @MojoProduces
  @Named("scmSshPassphrase")
  @Parameter(property = "unleash.scmSshPassphrase", required = false)
  private String scmSshPassphrase;

  @MojoProduces
  @Named("scmPasswordEnvVar")
  @Parameter(property = "unleash.scmPasswordEnvVar", required = false)
  private String scmPasswordEnvVar;

  @MojoProduces
  @Named("scmUsernameEnvVar")
  @Parameter(property = "unleash.scmUsernameEnvVar", required = false)
  private String scmUsernameEnvVar;

  @MojoProduces
  @Named("scmSshPassphraseEnvVar")
  @Parameter(property = "unleash.scmSshPassphraseEnvVar", required = false)
  private String scmSshPassphraseEnvVar;

  @MojoProduces
  @Named("scmSshPrivateKeyEnvVar")
  @Parameter(property = "unleash.scmSshPrivateKeyEnvVar", required = false)
  private String scmSshPrivateKeyEnvVar;

  @Parameter(property = "unleash.releaseEnvironment", required = false)
  private String releaseEnvironmentVariables;

  @MojoProduces
  @Parameter(property = "unleash.versionUpgradeStrategy", required = true, defaultValue = "DEFAULT")
  private VersionUpgradeStrategy versionUpgradeStrategy;

  @MojoProduces
  @Named("preserveFixedModuleVersions")
  @Parameter(property = "unleash.preserveFixedModuleVersions", required = false, defaultValue = "false")
  private boolean preserveFixedModuleVersions;

  @Parameter
  private Set<Repository> additionalDeploymentRepositories;

  @MojoProduces
  @Named("artifactSpyPlugin")
  private ArtifactCoordinates artifactSpyPluginCoordinates = new ArtifactCoordinates("com.itemis.maven.plugins",
      "artifact-spy-plugin", "1.0.6", "maven-plugin");

  @MojoProduces
  private PluginParameterExpressionEvaluator getExpressionEvaluator() {
    return new PluginParameterExpressionEvaluator(this.session, this.mojoExecution);
  }

  @MojoProduces
  private PluginDescriptor getPluginDescriptor() {
    return (PluginDescriptor) getPluginContext().get("pluginDescriptor");
  }

  @MojoProduces
  @Named("scmMessagePrefix")
  private String getScmMessagePrefix() {
    if (this.scmMessagePrefix != null && !this.scmMessagePrefix.endsWith(" ")) {
      this.scmMessagePrefix = this.scmMessagePrefix + " ";
    }
    return Strings.nullToEmpty(this.scmMessagePrefix);
  }

  @MojoProduces
  @Named("unleashOutputFolder")
  private File getUnleashOutputFolder() {
    File folder = new File(this.project.getBuild().getDirectory(), "unleash");
    folder.mkdirs();
    return folder;
  }

  @MojoProduces
  @Named("releaseArgs")
  @MojoInject
  private Properties getReleaseArgs(Log log) {
    Properties args = new Properties();
    Splitter splitter = Splitter.on('=');
    for (String arg : this.releaseArgs) {
      List<String> split = splitter.splitToList(arg);
      if (split.size() == 2) {
        args.put(split.get(0), split.get(1));
      } else {
        log.warn("Could not set '" + arg + "' as a Property for the Maven release build.");
      }
    }

    // Add default property indicating that the unleash plugin is triggering the build
    args.put("isUnleashBuild", "true");
    return args;
  }

  @MojoProduces
  @Named("releaseEnvVariables")
  private Map<String, String> getReleaseEnvironmentVariables() {
    Map<String, String> env = Maps.newHashMap();
    if (!Strings.isNullOrEmpty(this.releaseEnvironmentVariables)) {
      Iterable<String> split = Splitter.on(',').split(this.releaseEnvironmentVariables);
      for (String token : split) {
        String date = Strings.emptyToNull(token.trim());
        if (date != null) {
          List<String> dataSplit = Splitter.on("=>").splitToList(date);
          String key = dataSplit.get(0);
          String value = dataSplit.get(1);
          env.put(key, value);
        }
      }
    }
    return env;
  }

  @MojoProduces
  @Named("additionalDeployemntRepositories")
  private Set<RemoteRepository> getAdditionalDeploymentRepositories() {
    return this.additionalDeploymentRepositories.stream().map(repo -> {
      DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
      ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy();
      ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy();

      ArtifactRepository artifactRepository = new MavenArtifactRepository(repo.getId(), repo.getUrl(), layout,
          snapshotsPolicy, releasesPolicy);
      this.settings.getServers().stream().filter(server -> Objects.equals(server.getId(), repo.getId())).findFirst()
          .ifPresent(server -> artifactRepository.setAuthentication(createServerAuthentication(server)));
      return RepositoryUtils.toRepo(artifactRepository);
    }).collect(Collectors.toSet());
  }

  private Authentication createServerAuthentication(Server server) {
    Authentication authentication = new Authentication(server.getUsername(), server.getPassword());
    authentication.setPrivateKey(server.getPrivateKey());
    authentication.setPassphrase(server.getPassphrase());
    return authentication;
  }
}
