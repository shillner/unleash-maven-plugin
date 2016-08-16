package com.itemis.maven.plugins.unleash.scm.impl;

import java.io.File;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.itemis.maven.plugins.unleash.scm.ScmProviderInitialization;

public class DefaultScmProviderInitialization implements ScmProviderInitialization {
  private File workingDir;
  private Logger logger;
  private String username;
  private String password;
  private String sshPKPassphrase;

  public DefaultScmProviderInitialization(File workingDir) {
    Preconditions.checkArgument(workingDir != null, "The working directory for the SCM provider must be specified!");
    this.workingDir = workingDir;
  }

  @Override
  public File getWorkingDirectory() {
    return this.workingDir;
  }

  public DefaultScmProviderInitialization setUsername(String username) {
    this.username = username;
    return this;
  }

  @Override
  public Optional<String> getUsername() {
    return Optional.fromNullable(this.username);
  }

  public DefaultScmProviderInitialization setPassword(String password) {
    this.password = password;
    return this;
  }

  @Override
  public Optional<String> getPassword() {
    return Optional.fromNullable(this.password);
  }

  public DefaultScmProviderInitialization setSshPrivateKeyPassphrase(String sshPKPassphrase) {
    this.sshPKPassphrase = sshPKPassphrase;
    return this;
  }

  @Override
  public Optional<String> getSshPrivateKeyPassphrase() {
    return Optional.fromNullable(this.sshPKPassphrase);
  }

  public DefaultScmProviderInitialization setLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  @Override
  public Optional<Logger> getLogger() {
    return Optional.fromNullable(this.logger);
  }
}
