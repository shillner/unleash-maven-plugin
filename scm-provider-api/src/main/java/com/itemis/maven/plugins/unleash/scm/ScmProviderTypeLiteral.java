package com.itemis.maven.plugins.unleash.scm;

import javax.enterprise.util.AnnotationLiteral;

/**
 * The annotation literal for the {@link ScmProviderType} qualifier annotation.<br>
 * This literal is intended for internal use only (dynamic dispatching of scm provider types during the release
 * process).
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public class ScmProviderTypeLiteral extends AnnotationLiteral<ScmProviderType> implements ScmProviderType {
  private static final long serialVersionUID = -1508425066754437151L;
  private String scmName;

  public ScmProviderTypeLiteral(String scmName) {
    this.scmName = scmName;
  }

  @Override
  public String value() {
    return this.scmName;
  }
}
