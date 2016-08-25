package com.itemis.maven.plugins.unleash.util.scm;

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.annotations.ScmProviderTypeLiteral;
import com.itemis.maven.plugins.unleash.scm.impl.DefaultScmProviderInitialization;
import com.itemis.maven.plugins.unleash.util.logging.JavaLoggerAdapter;

/**
 * A singleton registry determining the correct {@link ScmProvider} implementation which is derived from the
 * {@link MavenProject} on which the release is started.<br>
 * The provider can be retrieved using {@link #getProvider()} and will be initialized previously.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
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
  @Inject
  @Named("scmSshPassphrase")
  private String scmSshPassphrase;
  @Inject
  @Named("scmUsernameEnvVar")
  private String scmUsernameEnvVar;
  @Inject
  @Named("scmPasswordEnvVar")
  private String scmPasswordEnvVar;
  @Inject
  @Named("scmSshPassphraseEnvVar")
  private String scmSshPassphraseEnvVar;
  private String scmProviderName;
  private ScmProvider provider;

  private ScmProviderRegistry() {
  }

  @PostConstruct
  private void init() {
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
    if (this.provider == null) {
      try {
        this.provider = this.providers.select(new ScmProviderTypeLiteral(this.scmProviderName)).get();
        checkProviderAPI();

        DefaultScmProviderInitialization initialization = new DefaultScmProviderInitialization(
            this.project.getBasedir());
        initialization.setLogger(new JavaLoggerAdapter(this.provider.getClass().getName(), this.log));
        initialization.setUsername(getScmUsername()).setPassword(getScmPassword())
            .setSshPrivateKeyPassphrase(getScmSshPassphrase());

        this.provider.initialize(initialization);
      } catch (IllegalStateException e) {
        throw e;
      } catch (Throwable t) {
        throw new IllegalStateException("No SCM provider found for SCM with name " + this.scmProviderName
            + ". Maybe you need to add an appropriate provider implementation as a dependency to the plugin.", t);
      }
    }
    return this.provider;
  }

  private void checkProviderAPI() throws IllegalStateException {
    boolean isIncompatible = false;
    Throwable cause = null;

    // compares all API methods against all implementation methods and fails on missing and/or wrong method signatures.
    for (Method apiMethod : ScmProvider.class.getDeclaredMethods()) {
      try {
        Method implMethod = this.provider.getClass().getDeclaredMethod(apiMethod.getName(),
            apiMethod.getParameterTypes());
        if (!Objects.equal(implMethod.getReturnType(), apiMethod.getReturnType())) {
          isIncompatible = true;
          break;
        }
      } catch (Throwable e) {
        isIncompatible = true;
        cause = e;
        break;
      }
    }

    if (isIncompatible) {
      this.log.error(
          "The SCM provider API and the configured implementation for SCMs with identifier '" + this.scmProviderName
              + "' are incompatible. Please check the compatibility notes of the chosen provider implementation.");
      if (cause != null) {
        throw new IllegalStateException(
            "Invalid SCM provider API version of provider implementation '" + this.scmProviderName + "'.", cause);
      } else {
        throw new IllegalStateException(
            "Invalid SCM provider API version of provider implementation '" + this.scmProviderName + "'.");
      }
    }
  }

  private String getScmUsername() {
    String username = Strings.emptyToNull(this.scmUsername);
    if (username == null && StringUtils.isNotBlank(this.scmUsernameEnvVar)) {
      username = Strings.emptyToNull(System.getenv(this.scmUsernameEnvVar));
    }
    return username;
  }

  private String getScmPassword() {
    String password = Strings.emptyToNull(this.scmPassword);
    if (password == null && StringUtils.isNotBlank(this.scmPasswordEnvVar)) {
      password = Strings.emptyToNull(System.getenv(this.scmPasswordEnvVar));
    }
    return password;
  }

  private String getScmSshPassphrase() {
    String passphrase = Strings.emptyToNull(this.scmSshPassphrase);
    if (passphrase == null && StringUtils.isNotBlank(this.scmSshPassphraseEnvVar)) {
      passphrase = Strings.emptyToNull(System.getenv(this.scmSshPassphraseEnvVar));
    }
    return passphrase;
  }

  @PreDestroy
  private void disposeProvider() {
    if (this.provider != null) {
      this.provider.close();
      this.provider = null;
    }
  }
}
