package com.itemis.maven.plugins.unleash;

import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.itemis.maven.aether.ArtifactCoordinates;

@Singleton
// TODO add serialization of metadata as a reporting feature!
public class ReleaseMetadata {
  private Map<ReleasePhase, String> scmRevisions;
  private Map<ReleasePhase, Set<ArtifactCoordinates>> artifactCoordinates;
  private String scmTagName;

  private ReleaseMetadata() {
    int numPhases = ReleasePhase.values().length;
    this.scmRevisions = Maps.newHashMapWithExpectedSize(numPhases);
    this.artifactCoordinates = Maps.newHashMapWithExpectedSize(numPhases);
    for (ReleasePhase phase : ReleasePhase.values()) {
      this.artifactCoordinates.put(phase, Sets.<ArtifactCoordinates> newHashSet());
    }
  }

  public void setScmRevision(String scmRevision, ReleasePhase phase) {
    this.scmRevisions.put(phase, scmRevision);
  }

  public String getScmRevision(ReleasePhase phase) {
    return this.scmRevisions.get(phase);
  }

  public void addArtifactCoordinates(ArtifactCoordinates coordinates, ReleasePhase phase) {
    this.artifactCoordinates.get(phase).add(coordinates);
  }

  public Map<ReleasePhase, ArtifactCoordinates> getArtifactCoordinatesByPhase(String groupId, String artifactId) {
    Map<ReleasePhase, ArtifactCoordinates> result = Maps.newHashMapWithExpectedSize(this.artifactCoordinates.size());
    for (ReleasePhase phase : this.artifactCoordinates.keySet()) {
      for (ArtifactCoordinates coordinates : this.artifactCoordinates.get(phase)) {
        if (Objects.equal(coordinates.getArtifactId(), artifactId)
            && Objects.equal(coordinates.getGroupId(), groupId)) {
          result.put(phase, coordinates);
          break;
        }
      }
    }
    return result;
  }

  public void setScmTagName(String scmTagName) {
    this.scmTagName = scmTagName;
  }

  public String getScmTagName() {
    return this.scmTagName;
  }
}
