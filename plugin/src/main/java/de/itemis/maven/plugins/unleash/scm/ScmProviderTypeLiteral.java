package de.itemis.maven.plugins.unleash.scm;

import javax.enterprise.util.AnnotationLiteral;

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
