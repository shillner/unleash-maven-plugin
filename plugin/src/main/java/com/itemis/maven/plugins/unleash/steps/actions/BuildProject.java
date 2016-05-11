package com.itemis.maven.plugins.unleash.steps.actions;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.google.common.collect.Lists;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

@ProcessingStep(id = "buildReleaseArtifacts", description = "Triggers the atual release build (clean verify) which produces the artifacts for later installation and deployment.")
public class BuildProject implements CDIMojoProcessingStep {
  @Inject
  private MavenLogWrapper log;

  @Inject
  private MavenProject project;

  @Inject
  @Named("profiles")
  private List<String> profiles;

  @Inject
  @Named("maven.home")
  private String mavenHome;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(this.project.getFile());
    // installation and deployment are performed in a later step. We first need to ensure that there are no changes in
    // the scm, ...
    request.setGoals(Lists.newArrayList("clean", "verify"));
    request.setProfiles(this.profiles);
    request.setShellEnvironmentInherited(true);

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
      this.log.debug("Using maven home: " + path);
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
