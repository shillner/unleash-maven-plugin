package com.itemis.maven.plugins.unleash.scm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A CDI qualifier annotation for the qualification of beans implementing the {@link ScmProvider} interface. This
 * qualifier is used to dispatch different SCM implementations at runtime.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface ScmProviderType {
  /**
   * Provides the name of the SCM implementation. Note that this name must match the name provided by the connections of
   * the scm-section provided by the pom.<br>
   * <b>F.i. use <i>{@code svn}</i> or <i>{@code git}</i>.</b>
   *
   * @return the name of the scm this provider is implemented for.
   */
  String value();
}
