package com.itemis.maven.plugins.unleash.util.scm;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.annotations.ScmProviderTypeLiteral;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@Singleton
public class ScmProviderRegistry {
  @Inject
  private MavenLogWrapper log;

  @Inject
  @Any
  private Instance<ScmProvider> providers;

  @Inject
  private MavenProject project;

  @Inject
  @Named("scmUsername")
  private String scmUsername;

  @Inject
  @Named("scmPassword")
  private String scmPassword;

  private String scmProviderName;
  private ScmProvider provider;

  ScmProviderRegistry() {
  }

  @PostConstruct
  private void init() {
    // TODO find a way to detect the API version the scmProvider is implementing! must match the version the plugin
    // provides (bugfix version diffs are ok)
    Optional<String> providerName = MavenScmUtil.calcProviderName(this.project);
    if (!providerName.isPresent()) {
      this.log.error(
          "Could not determine SCM provider name from your POM configuration! Please check the SCM section of your POM and provide connections in the correct format (see also: https://maven.apache.org/scm/scm-url-format.html).");
    } else {
      this.log.debug("Resolved required SCM provider implementation to '" + providerName.get() + "'");
    }
    this.scmProviderName = providerName.orNull();
  }

  public Optional<ScmProvider> getProvider() {
    try {
      this.provider = this.providers.select(new ScmProviderTypeLiteral(this.scmProviderName)).get();
      this.provider.initialize(this.project.getBasedir(), Optional.fromNullable(this.scmUsername),
          Optional.fromNullable(this.scmPassword));
    } catch (Throwable t) {
      this.log.warn("No SCM provider found for SCM with name " + this.scmProviderName, t);
    }

    return Optional.fromNullable(this.provider);
  }

  @PreDestroy
  private void disposeProvider() {
    if (this.provider != null) {
      this.provider.close();
      this.provider = null;
    }
  }
}
