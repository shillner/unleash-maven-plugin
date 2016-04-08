package com.itemis.maven.plugins.unleash.actions;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.cdi.InjectableCdiMojo;
import com.itemis.maven.plugins.cdi.annotations.MojoExecution;
import com.itemis.maven.plugins.unleash.ReleaseMetadata;
import com.itemis.maven.plugins.unleash.scm.MavenScmUtil;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.ScmProviderRegistry;
import com.itemis.maven.plugins.unleash.util.MavenLogWrapper;

/**
 * A Mojo that just stores the local SCM revision information in the release metadata. This information is needed in a
 * later step to ensure that no other commits where done while releasing the artifact.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@MojoExecution(name = "perform", order = 0)
public class StoreScmRevision implements InjectableCdiMojo {
  @Inject
  private MavenLogWrapper log;
  @Inject
  private MavenProject project;
  @Inject
  private ScmProviderRegistry providerRegistry;
  @Inject
  private ReleaseMetadata metadata;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Optional<String> providerName = MavenScmUtil.getScmProviderName(this.project);
    if (!providerName.isPresent()) {
      throw new MojoFailureException(
          "Could not determine SCM provider name from your POM configuration! Please check the SCM section of your POM and provide connections in the correct format (see also: https://maven.apache.org/scm/scm-url-format.html).");
    }
    this.log.debug("Resolved required SCM provider implementation to '" + providerName + "'");

    Optional<ScmProvider> provider = this.providerRegistry.getScmProvider(providerName.get());
    if (!provider.isPresent()) {
      throw new MojoFailureException("Could not load an SCM provider for provider name '" + providerName
          + "'. Maybe you need to add an appropriate provider implementation as a dependency to the plugin.");
    }

    String revision = provider.get().getLocalRevision();
    this.metadata.setPreReleaseScmRevision(revision);
    this.log.info("SCM Revision before releasing the artifacts: " + revision);
  }
}
