package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Plugin;

import com.google.common.base.Function;

public enum PluginToString implements Function<Plugin, String> {
  INSTANCE;

  @Override
  public String apply(Plugin p) {
    StringBuilder sb = new StringBuilder(p.getGroupId());
    sb.append(":").append(p.getArtifactId()).append(":").append(p.getVersion());
    return sb.toString();
  }
}
