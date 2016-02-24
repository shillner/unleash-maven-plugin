package de.itemis.maven.plugins.unleash;

public interface UnleashProcessingAction {
  void prepare();

  void execute();

  void rollback();
}
