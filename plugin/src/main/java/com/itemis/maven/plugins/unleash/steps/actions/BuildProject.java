package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.cli.CommandLineException;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.logging.Logger;
import com.itemis.maven.plugins.unleash.util.ReleaseUtil;

/**
 * Performs the actual release build but does not install or deploy artifacts to the repositories. These steps are
 * performed later after all other steps, such as SCM tagging aso. have finished.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@ProcessingStep(id = "buildReleaseArtifacts", description = "Triggers the atual release build which produces the release artifacts but does not install or deploy them.", requiresOnline = true)
public class BuildProject implements CDIMojoProcessingStep {
  @Inject
  private Logger log;
  @Inject
  private MavenProject project;
  @Inject
  @Named("profiles")
  private List<String> profiles;
  @Inject
  @Named("maven.home")
  private String mavenHome;
  @Inject
  private Settings settings;
  @Inject
  @Named("releaseArgs")
  private String releaseArgs;
  @Inject
  @Named("unleashOutputFolder")
  private File unleashOutputFolder;

  private File tempSettingsFile;

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Starting release build.");

    try {
      InvocationRequest request = setupInvocationRequest();
      Invoker invoker = setupInvoker();

      InvocationResult result = invoker.execute(request);
      if (result.getExitCode() != 0) {
        CommandLineException executionException = result.getExecutionException();
        if (executionException != null) {
          throw new MojoFailureException("Error during project build: " + executionException.getMessage(),
              executionException);
        } else {
          throw new MojoFailureException("Error during project build: " + result.getExitCode());
        }
      }
    } catch (MavenInvocationException e) {
      throw new MojoFailureException(e.getMessage(), e);
    } finally {
      deleteTempSettings();
    }
  }

  private Invoker setupInvoker() {
    Invoker invoker = new DefaultInvoker();
    File calculatedMavenHome = ReleaseUtil.getMavenHome(Optional.fromNullable(this.mavenHome));
    if (calculatedMavenHome != null) {
      this.log.debug("\tUsing maven home: " + calculatedMavenHome.getAbsolutePath());
      invoker.setMavenHome(calculatedMavenHome);
    }
    return invoker;
  }

  private InvocationRequest setupInvocationRequest() throws MojoExecutionException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(this.project.getFile());
    // installation and deployment are performed in a later step. We first need to ensure that there are no changes in
    // the scm, ...
    request.setGoals(Lists.newArrayList("clean", "verify"));
    request.setMavenOpts(this.releaseArgs);
    request.setProfiles(this.profiles);
    request.setShellEnvironmentInherited(true);
    request.setOffline(this.settings.isOffline());
    request.setInteractive(this.settings.isInteractiveMode());
    this.tempSettingsFile = createAndSetTempSettings(request);
    return request;
  }

  private File createAndSetTempSettings(InvocationRequest request) throws MojoExecutionException {
    SettingsWriter settingsWriter = new DefaultSettingsWriter();
    File settingsFile = new File(this.unleashOutputFolder, "settings.xml");
    try {
      settingsWriter.write(settingsFile, null, this.settings);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to store Maven settings for release build", e);
    }
    request.setUserSettingsFile(settingsFile);
    return settingsFile;
  }

  private void deleteTempSettings() {
    if (this.tempSettingsFile != null && this.tempSettingsFile.exists()) {
      this.tempSettingsFile.delete();
      this.tempSettingsFile = null;
    }
  }

  @RollbackOnError
  public void rollback() {
    deleteTempSettings();
  }
}
