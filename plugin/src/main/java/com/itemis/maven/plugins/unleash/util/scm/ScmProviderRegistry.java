package com.itemis.maven.plugins.unleash.util.scm;

import java.util.logging.Logger;

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

@Singleton
public class ScmProviderRegistry {
  @Inject
  private com.itemis.maven.plugins.cdi.logging.Logger log;

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

  private ScmProviderRegistry() {
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

  public ScmProvider getProvider() throws IllegalStateException {
    try {
      this.provider = this.providers.select(new ScmProviderTypeLiteral(this.scmProviderName)).get();
      this.provider.initialize(this.project.getBasedir(), Optional.<Logger> absent(),
          Optional.fromNullable(this.scmUsername), Optional.fromNullable(this.scmPassword));
    } catch (Throwable t) {
      throw new IllegalStateException("No SCM provider found for SCM with name " + this.scmProviderName
          + ". Maybe you need to add an appropriate provider implementation as a dependency to the plugin.", t);
    }
    return this.provider;
  }

  @PreDestroy
  private void disposeProvider() {
    if (this.provider != null) {
      this.provider.close();
      this.provider = null;
    }
  }
}
