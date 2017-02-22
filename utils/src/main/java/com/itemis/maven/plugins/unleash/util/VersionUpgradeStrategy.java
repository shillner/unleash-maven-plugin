package com.itemis.maven.plugins.unleash.util;

public enum VersionUpgradeStrategy {
  MAJOR((short) 0), MINOR((short) 1), INCREMENTAL((short) 2), DEFAULT((short) -1);

  private short versionSegmentIndex;

  private VersionUpgradeStrategy(short index) {
    this.versionSegmentIndex = index;
  }

  public short getVersionSegmentIndex() {
    return this.versionSegmentIndex;
  }
}
