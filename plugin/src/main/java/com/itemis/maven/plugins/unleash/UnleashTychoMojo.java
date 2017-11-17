package com.itemis.maven.plugins.unleash;

import org.apache.maven.plugins.annotations.Mojo;

import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;

/**
 * A Maven {@link Mojo} which performs a release of the project it is started on.<br>
 * Release means that the versions of all modules are nailed down to real release versions so that the artifacts are
 * stable and reproducible.
 * Furthermore the whole project is built with these versions, SCM tags will be created and the artifacts will be
 * installed and deployed.<br>
 * <br>
 * Since this mojo depends on the base mojo of the <a href="https://github.com/shillner/maven-cdi-plugin-utils">CDI
 * Plugin Utils</a>
 * project it implements a basic workflow which is fully configurable and extendable by nature.<br>
 * <br>
 * In order to get this plugin to work you will have to add the appropriate SCM Provider implementation as a plugin
 * dependency, such as <a href="https://github.com/shillner/unleash-scm-provider-svn">SVN provider</a> or
 * <a href="https://github.com/shillner/unleash-scm-provider-git">Git provider</a>.<br>
 * You may also add further plugin dependencies that provider some additional {@link ProcessingStep} implementations you
 * want to use in your adapted workflow, f.i. <a href="https://github.com/shillner/maven-cdi-plugin-hooks">CDI Plugin
 * Hooks</a>.
 *
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Mojo(name = "perform-tycho", aggregator = true, requiresProject = true)
public class UnleashTychoMojo extends AbstractUnleashMojo {
}
