package com.itemis.maven.plugins.unleash.scm;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.base.Optional;

@ApplicationScoped
public class ScmProviderRegistry {
  // @Inject
  // private MavenLogWrapper log;

  @Inject
  @Any
  private Instance<ScmProvider> providers;

  ScmProviderRegistry() {
  }

  public Optional<ScmProvider> getScmProvider(String scmName) {
    ScmProvider scmProvider;
    try {
      scmProvider = this.providers.select(new ScmProviderTypeLiteral(scmName)).get();
    } catch (Throwable t) {
      scmProvider = null;
      // this.log.debug("No SCM provider found for SCM with name " + scmName, t);
    }
    return Optional.fromNullable(scmProvider);
  }
}
