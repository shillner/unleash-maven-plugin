package com.itemis.maven.plugins.unleash;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReleaseMetadata {
  private String preReleaseScmRevision;
  // private List<ArtifactCoordinates> preReleaseProjectCoordinates;
  // private List<ArtifactCoordinates> releaseProjectCoodinates;

  private ReleaseMetadata() {
  }

  public void setPreReleaseScmRevision(String preReleaseScmRevision) {
    this.preReleaseScmRevision = preReleaseScmRevision;
  }

  public String getPreReleaseScmRevision() {
    return this.preReleaseScmRevision;
  }

  // public void setPreReleaseProjectCoordinates(List<ArtifactCoordinates> preReleaseProjectCoordinates) {
  // this.preReleaseProjectCoordinates = preReleaseProjectCoordinates;
  // }
  //
  // public List<ArtifactCoordinates> getPreReleaseProjectCoordinates() {
  // return this.preReleaseProjectCoordinates;
  // }
  //
  // public void setReleaseProjectCoodinates(List<ArtifactCoordinates> releaseProjectCoodinates) {
  // this.releaseProjectCoodinates = releaseProjectCoodinates;
  // }
  //
  // public List<ArtifactCoordinates> getReleaseProjectCoodinates() {
  // return this.releaseProjectCoodinates;
  // }
}
