package com.itemis.maven.plugins.unleash.util.scm;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.w3c.dom.Document;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.unleash.scm.ScmException;
import com.itemis.maven.plugins.unleash.scm.ScmOperation;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomUtil;

public class ScmPomVersionsMergeClient implements MergeClient {

  @Override
  public void merge(File local, File remote, File base, OutputStream result) throws ScmException {
    Optional<Model> localModel = loadModel(local);
    if (!localModel.isPresent()) {
      // TODO implement merge of other files!
      throw new ScmException(ScmOperation.MERGE, "Unable to merge non-POM changes.");
    }

    Optional<Model> remoteModel = loadModel(remote);
    Optional<Model> baseModel = loadModel(base);

    Model resultModel = loadModel(remote).get();
    mergeVersions(localModel.get(), remoteModel.get(), baseModel.get(), resultModel);
    mergeParentVersions(localModel.get(), remoteModel.get(), baseModel.get(), resultModel);

    try {
      Document document = PomUtil.parsePOM(remote);
      PomUtil.setProjectVersion(resultModel, document, resultModel.getVersion());
      if (resultModel.getParent() != null) {
        PomUtil.setParentVersion(resultModel, document, resultModel.getParent().getVersion());
      }
      PomUtil.writePOM(document, result, false);
    } catch (Throwable t) {
      throw new ScmException(ScmOperation.MERGE, "Could not serialize merged POM!", t);
    }
  }

  private void mergeVersions(Model local, Model remote, Model base, Model result) throws ScmException {
    boolean remoteChange = !Objects.equal(remote.getVersion(), base.getVersion());
    if (remoteChange) {
      if (!Objects.equal(local.getVersion(), remote.getVersion())) {
        if (MavenVersionUtil.isNewerVersion(local.getVersion(), remote.getVersion())) {
          result.setVersion(local.getVersion());
        } else {
          throw new ScmException(ScmOperation.MERGE,
              "Could not merge POM version changes since the new remote version is higher than the local one!");
        }
      }
    } else {
      result.setVersion(local.getVersion());
    }
  }

  private void mergeParentVersions(Model local, Model remote, Model base, Model result) throws ScmException {
    String localParentVersion = local.getParent() != null ? local.getParent().getVersion() : null;
    String remoteParentVersion = remote.getParent() != null ? remote.getParent().getVersion() : null;
    String baseParentVersion = base.getParent() != null ? base.getParent().getVersion() : null;

    boolean remoteParentRemoved = remoteParentVersion == null && baseParentVersion != null;
    boolean remoteParentAdded = remoteParentVersion != null && baseParentVersion == null;
    boolean remoteParentVersionChanged = !Objects.equal(remoteParentVersion, baseParentVersion);

    boolean localParentRemoved = localParentVersion == null && baseParentVersion != null;
    boolean localParentAdded = localParentVersion != null && baseParentVersion == null;
    boolean localParentVersionChanged = !Objects.equal(localParentVersion, baseParentVersion);

    if (localParentAdded) {
      // if locally added the base had no parent (remote remove and change is not relevant)
      if (remoteParentAdded) {
        if (Objects.equal(local.getParent().getArtifactId(), remote.getParent().getArtifactId())
            && Objects.equal(local.getParent().getGroupId(), remote.getParent().getGroupId())) {
          if (MavenVersionUtil.isNewerVersion(local.getParent().getVersion(), remote.getParent().getVersion())) {
            result.setParent(local.getParent());
          }
        } else {
          throw new ScmException(ScmOperation.MERGE,
              "Could not merge local and remote POM parent changes since both versions added different parent artifacts.");
        }
      } else {
        result.setParent(local.getParent());
      }
    } else if (localParentRemoved) {
      // if locally removed the base had a parent (remote add is not relevant and remote remove is ok)
      if (remoteParentVersionChanged) {
        throw new ScmException(ScmOperation.MERGE,
            "Could not merge POM parent version conflicts since in the local POM the parent had been removed and in the remote POM the parent had been changed.");
      } else {
        result.getParent().setVersion(localParentVersion);
      }
    } else if (localParentVersionChanged) {
      // if locally changed the base had a parent (remote add is not relevant)
      if (remoteParentVersionChanged) {
        if (Objects.equal(local.getParent().getArtifactId(), remote.getParent().getArtifactId())
            && Objects.equal(local.getParent().getGroupId(), remote.getParent().getGroupId())) {
          if (MavenVersionUtil.isNewerVersion(local.getParent().getVersion(), remote.getParent().getVersion())) {
            result.setParent(local.getParent());
          }
        } else {
          throw new ScmException(ScmOperation.MERGE,
              "Could not merge local and remote POM parent changes since both versions are referencing different parent artifacts.");
        }
      } else if (remoteParentRemoved) {
        throw new ScmException(ScmOperation.MERGE,
            "Could not merge POM parent version conflicts since in the local POM the parent had been updated while in the remote POM the parent had been removed.");
      } else {
        result.getParent().setVersion(localParentVersion);
      }
    }
  }

  private Optional<Model> loadModel(File f) {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    FileReader fr = null;
    try {
      fr = new FileReader(f);
      Model model = reader.read(fr);
      return Optional.of(model);
    } catch (Exception e) {
      return Optional.absent();
    } finally {
      Closeables.closeQuietly(fr);
    }
  }
}
