package de.itemis.maven.plugins.unleash.util;

import java.util.List;

import de.itemis.maven.aether.ArtifactCoordinates;

public class ReleaseMetadataManager {
  private static ReleaseMetadataManager instance;

  private String preReleaseScmRevision;
  private List<ArtifactCoordinates> preReleaseProjectCoordinates;
  private List<ArtifactCoordinates> releaseProjectCoodinates;

  private ReleaseMetadataManager() {
  }

  public static ReleaseMetadataManager getInstance() {
    if (instance == null) {
      instance = new ReleaseMetadataManager();
    }
    return instance;
  }

  public void setPreReleaseScmRevision(String preReleaseScmRevision) {
    this.preReleaseScmRevision = preReleaseScmRevision;
  }

  public String getPreReleaseScmRevision() {
    return this.preReleaseScmRevision;
  }

  public void setPreReleaseProjectCoordinates(List<ArtifactCoordinates> preReleaseProjectCoordinates) {
    this.preReleaseProjectCoordinates = preReleaseProjectCoordinates;
  }

  public List<ArtifactCoordinates> getPreReleaseProjectCoordinates() {
    return this.preReleaseProjectCoordinates;
  }

  public void setReleaseProjectCoodinates(List<ArtifactCoordinates> releaseProjectCoodinates) {
    this.releaseProjectCoodinates = releaseProjectCoodinates;
  }

  public List<ArtifactCoordinates> getReleaseProjectCoodinates() {
    return this.releaseProjectCoodinates;
  }
}
