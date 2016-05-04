package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import com.google.common.base.Function;
import com.itemis.maven.plugins.unleash.util.PomUtil;

public enum ProjectToXmlDocument implements Function<MavenProject, Document> {
  INSTANCE;

  @Override
  public Document apply(MavenProject project) {
    return PomUtil.parsePOM(project);
  }
}
