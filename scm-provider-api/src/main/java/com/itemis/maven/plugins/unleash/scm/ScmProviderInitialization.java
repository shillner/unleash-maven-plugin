package com.itemis.maven.plugins.unleash.scm;

import java.io.File;
import java.util.logging.Logger;

import com.google.common.base.Optional;

public interface ScmProviderInitialization {
  File getWorkingDirectory();

  Optional<String> getUsername();

  Optional<String> getPassword();

  Optional<String> getSshPrivateKeyPassphrase();

  Optional<Logger> getLogger();
}
