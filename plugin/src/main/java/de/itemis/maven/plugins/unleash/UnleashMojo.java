package de.itemis.maven.plugins.unleash;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import com.google.common.base.Optional;

import de.itemis.maven.aether.ArtifactResolver;

@Mojo(name = "perform", aggregator = true, requiresProject = true)
public class UnleashMojo extends AbstractMojo {
  private static final String VERSION_MODIFIER_SNAPSHOT = "-SNAPSHOT";

  @Component
  private RepositorySystem repositorySystem;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repoSession;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  private List<MavenProject> reactorProjects;

  @Parameter(name = "developmentVersion")
  private String developmentVersion;

  @Parameter(name = "releaseVersion")
  private String releaseVersion;

  // TODO group actions into steps and execute steps transactionally as an atomic operation
  public void execute() throws MojoExecutionException, MojoFailureException {
    for (MavenProject project : reactorProjects) {
      Model projectModel = project.getModel();
      updateVersion(projectModel);
      updateParentVersion(projectModel);

      if (checkIfAlreadyReleased(project)) {
        throw new MojoFailureException("The module " + project.getGroupId() + ":" + project.getArtifactId() + ":"
            + project.getVersion() + " has already been released. Check your aritfact repositories!");
      }

      try {
        // loads a plain model from the project file since project.getModel() would return a pom with mixed-in parent
        // declarations and would thus not be suitable for writing back
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new InputStreamReader(new FileInputStream(project.getFile())));
        if (model.getVersion() != null) {
          model.setVersion(projectModel.getVersion());
        }
        if (model.getParent() != null) {
          model.getParent().setVersion(projectModel.getParent().getVersion());
        }

        // TODO maybe do not just serialize the model but adapt the versions by hand to preserve the exact formatting of
        // the pom
        new DefaultModelWriter().write(new File(project.getFile().getAbsolutePath().concat("_new")), null, model);
      } catch (Exception e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    }
  }

  private void updateVersion(Model model) {
    if (model.getVersion() == null) {
      // in this case it is not necessary to update the version since the version is inherited from the parent
      return;
    }

    if (releaseVersion != null) {
      model.setVersion(releaseVersion);
    } else {
      // TODO handle cases where version does not end on -SNAPSHOT
      String currentVersion = model.getVersion();
      if (currentVersion.endsWith(VERSION_MODIFIER_SNAPSHOT)) {
        String newVersion = currentVersion.substring(0, currentVersion.length() - VERSION_MODIFIER_SNAPSHOT.length());
        model.setVersion(newVersion);
      }
    }
  }

  private void updateParentVersion(Model model) {
    Parent parent = model.getParent();
    if (parent == null) {
      return;
    }

    for (MavenProject project : reactorProjects) {
      if (project.getArtifactId().equals(parent.getArtifactId()) && project.getGroupId().equals(parent.getGroupId())) {
        parent.setVersion(project.getVersion());
        break;
      }
    }
  }

  private boolean checkIfAlreadyReleased(MavenProject project) {
    Optional<File> pom = ArtifactResolver.resolveArtifact(project.getGroupId(), project.getArtifactId(),
        project.getVersion(), Optional.of("pom"), Optional.<String> absent(), Optional.<String> absent(), getLog());
    return pom.isPresent();
  }
}
