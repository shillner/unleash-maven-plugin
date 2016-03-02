package de.itemis.maven.plugins.unleash;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.itemis.maven.plugins.unleash.actions.AbstractProcessingAction;
import de.itemis.maven.plugins.unleash.actions.CheckAether;
import de.itemis.maven.plugins.unleash.actions.CheckReleasable;
import de.itemis.maven.plugins.unleash.actions.CheckScmRevision;
import de.itemis.maven.plugins.unleash.util.MavenLogWrapper;
import de.itemis.maven.plugins.unleash.util.cdi.CdiBeanWrapper;
import de.itemis.maven.plugins.unleash.util.cdi.MojoCdiProducer;
import de.itemis.maven.plugins.unleash.util.cdi.WithCdiInjection;

@Mojo(name = "perform", aggregator = true, requiresProject = true)
public class UnleashMojo extends AbstractMojo implements Extension {
  @Component
  @MojoCdiProducer
  public RepositorySystem repoSystem;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  @MojoCdiProducer
  public RepositorySystemSession repoSession;

  @Parameter(readonly = true, defaultValue = "${project.remotePluginRepositories}")
  @MojoCdiProducer
  @Named("pluginRepositories")
  public List<RemoteRepository> remotePluginRepos;

  @Parameter(readonly = true, defaultValue = "${project.remoteProjectRepositories}")
  @MojoCdiProducer
  @Named("projectRepositories")
  public List<RemoteRepository> remoteProjectRepos;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  @MojoCdiProducer
  @Named("reactorProjects")
  private List<MavenProject> reactorProjects;

  @Parameter
  @MojoCdiProducer
  @Named("developmentVersion")
  private String developmentVersion;

  @Parameter
  @MojoCdiProducer
  @Named("releaseVersion")
  private String releaseVersion;

  @Parameter(defaultValue = "true", property = "unleash.logTimestamps")
  @MojoCdiProducer
  @Named("enableLogTimestamps")
  private boolean enableLogTimestamps;

  @Parameter(defaultValue = "true", property = "unleash.allowLocalReleaseArtifacts")
  @MojoCdiProducer
  @Named("allowLocalReleaseArtifacts")
  private boolean allowLocalReleaseArtifacts;

  private MavenLogWrapper logger;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Weld weld = new Weld();
    weld.addExtension(this);
    WeldContainer weldContainer = weld.initialize();

    // init();
    //
    // List<UnleashProcessingAction> actions = setupActions();
    // for (UnleashProcessingAction action : actions) {
    // action.prepare();
    // action.execute();
    // }

    weldContainer.shutdown();
  }

  private void init() {
    this.logger = new MavenLogWrapper(getLog());
    if (this.enableLogTimestamps) {
      this.logger.enableLogTimestamps();
    }
  }

  @MojoCdiProducer
  @WithCdiInjection
  public MavenLogWrapper createLogWrapper() {// TODO implement parameter injection@Named("x") String x) {//
                                             // @Named("allowLocalReleaseArtifacts") boolean
    MavenLogWrapper log = new MavenLogWrapper(getLog());
    if (this.enableLogTimestamps) {
      log.enableLogTimestamps();
    }
    return log;
  }

  private List<UnleashProcessingAction> setupActions() {
    this.logger.debug("Initializing the release actions.");
    List<UnleashProcessingAction> actions = Lists.newArrayList();

    // QUESTION how can we describe the workflow and setup the actions in another, more flexible way?
    // users should be able to include and exclude actions that are not needed!
    CheckScmRevision checkScmRevision = new CheckScmRevision();
    addCommonActionParams(checkScmRevision);
    actions.add(checkScmRevision);

    CheckReleasable releasableCheckAction = new CheckReleasable();
    addCommonActionParams(releasableCheckAction);
    actions.add(releasableCheckAction);

    CheckAether checkAetherAction = new CheckAether(this.releaseVersion);
    addCommonActionParams(checkAetherAction);
    checkAetherAction.allowLocalReleaseArtifacts(this.allowLocalReleaseArtifacts);
    actions.add(checkAetherAction);

    this.logger.debug("Release action order:");
    for (UnleashProcessingAction action : actions) {
      this.logger.debug("\t" + action.getClass().getSimpleName());
    }
    this.logger.debug("");
    return actions;
  }

  private void addCommonActionParams(AbstractProcessingAction action) {
    action.setLog(this.logger);
    action.setReactorProjects(this.reactorProjects);
    action.setRemoteProjectRepos(this.remoteProjectRepos);
    action.setRepoSession(this.repoSession);
    action.setRepoSystem(this.repoSystem);
  }

  public void processMojoCdiProducerFields(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    for (Field f : getClass().getDeclaredFields()) {
      if (f.isAnnotationPresent(MojoCdiProducer.class)) {
        try {
          event.addBean(new CdiBeanWrapper<Object>(f.get(this), f.getGenericType(), f.getType(), getCdiQualifiers(f)));
        } catch (Throwable t) {
          // FIXME handle and log!
          throw new RuntimeException(t);
        }
      }
    }
  }

  public void processMojoCdiProducerMethods(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    for (Method m : getClass().getDeclaredMethods()) {
      if (m.getReturnType() != Void.class && m.isAnnotationPresent(MojoCdiProducer.class)) {
        try {
          // FIXME: implement parameter injection for producer methods!
          // Object[] params = new Object[0];
          //
          // // TODO parameter injection!?!
          // java.lang.reflect.Parameter[] parameters = m.getParameters();
          // if (parameters.length > 0) {
          // params = new Object[parameters.length];
          // if (m.isAnnotationPresent(WithCdiInjection.class)) {
          // for (int i = 0; i < parameters.length; i++) {
          // java.lang.reflect.Parameter p = parameters[i];
          // Set<Annotation> qualifiers = getCdiQualifiers(p);
          //
          // params[i] = beanManager.getBeans(p.getType(), qualifiers.toArray(new Annotation[qualifiers.size()]));
          // }
          // } else {
          // // FIXME handle -> not parameter injection requested!
          // }
          // }
          //
          // event.addBean(new CdiBeanWrapper<Object>(m.invoke(this, params), m.getGenericReturnType(),
          // m.getReturnType(),
          // getCdiQualifiers(m)));
          event.addBean(new CdiBeanWrapper<Object>(m.invoke(this), m.getGenericReturnType(), m.getReturnType(),
              getCdiQualifiers(m)));
        } catch (Throwable t) {
          // FIXME handle and log!
          throw new RuntimeException(t);
        }
      }
    }
  }

  private Set<Annotation> getCdiQualifiers(AccessibleObject x) {
    Set<Annotation> qualifiers = Sets.newHashSet();
    for (Annotation annotation : x.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifiers.add(annotation);
      }
    }
    if (qualifiers.isEmpty()) {
      qualifiers.add(DefaultLiteral.INSTANCE);
    }
    return qualifiers;
  }

  private Set<Annotation> getCdiQualifiers(java.lang.reflect.Parameter p) {
    Set<Annotation> qualifiers = Sets.newHashSet();
    for (Annotation annotation : p.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifiers.add(annotation);
      }
    }
    if (qualifiers.isEmpty()) {
      qualifiers.add(DefaultLiteral.INSTANCE);
    }
    return qualifiers;
  }
}
