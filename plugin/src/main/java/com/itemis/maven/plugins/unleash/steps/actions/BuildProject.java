package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.google.common.collect.Lists;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.logging.Logger;

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

  @Override
  public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
    this.log.info("Starting release build.");

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

    // IDEA outsource maven executions to an injectable executor since this part will also be needed in later steps
    Invoker invoker = new DefaultInvoker();
    setMavenHome(invoker);
    try {
      InvocationResult result = invoker.execute(request);
      if (result.getExitCode() != 0) {
        throw new MojoFailureException("Error during project build: " + result.getExecutionException().getMessage());
      }
    } catch (MavenInvocationException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  private void setMavenHome(Invoker invoker) {
    String path = null;
    if (isValidMavenHome(this.mavenHome)) {
      path = this.mavenHome;
    } else {
      String sysProp = System.getProperty("maven.home");
      if (isValidMavenHome(sysProp)) {
        path = sysProp;
      } else {
        String envVar = System.getenv("M2_HOME");
        if (isValidMavenHome(envVar)) {
          path = envVar;
        }
      }
    }

    if (path != null) {
      this.log.debug("\tUsing maven home: " + path);
      invoker.setMavenHome(new File(path));
    }
  }

  private boolean isValidMavenHome(String path) {
    if (path != null) {
      File homeFolder = new File(path);
      return homeFolder.exists() && homeFolder.isDirectory() && new File(homeFolder, "bin/mvn").exists();
    }
    return false;
  }
}
