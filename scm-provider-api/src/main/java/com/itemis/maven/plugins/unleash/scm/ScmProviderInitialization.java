package com.itemis.maven.plugins.unleash.scm;

import java.io.File;
import java.util.logging.Logger;

import com.google.common.base.Optional;

/**
 * A wrapper for initialization parameters for {@link ScmProvider ScmProviders}.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public interface ScmProviderInitialization {
  /**
   * @return The working directory of the provider instance. This directory can either exist or not and is the point
   *         where the provider shall checkout things and work on.
   */
  File getWorkingDirectory();

  /**
   * @return an optional username for user/password authentication.
   */
  Optional<String> getUsername();

  /**
   * @return an optional password for user/password authentication.
   */
  Optional<String> getPassword();

  /**
   * @return an optional passphrase that can be used for public key authentication when using SSH.
   */
  Optional<String> getSshPrivateKeyPassphrase();

  /**
   * @return an optional private key to use for SSH-based SCM access.
   */
  Optional<String> getSshPrivateKey();

  /**
   * @return an optional logger for the provider.
   */
  Optional<Logger> getLogger();
}
