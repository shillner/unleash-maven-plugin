package com.itemis.maven.plugins.unleash.util.scm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.w3c.dom.Document;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.unleash.scm.ScmException;
import com.itemis.maven.plugins.unleash.scm.ScmOperation;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.itemis.maven.plugins.unleash.util.MavenVersionUtil;
import com.itemis.maven.plugins.unleash.util.PomUtil;

/**
 * An implementation of the {@link MergeClient} used while committing changes during the release process.<br>
 * This merge client is only able to merge versions and parent versions if conflicting POM files. This is sufficient at
 * this point as in the release process other conflicts should not arise.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public class ScmPomVersionsMergeClient implements MergeClient {

  @Override
  public void merge(InputStream local, InputStream remote, InputStream base, OutputStream result) throws ScmException {
    Optional<Model> localModel = loadModel(local);
    if (!localModel.isPresent()) {
      // TODO implement merge of other files!
      throw new ScmException(ScmOperation.MERGE, "Unable to merge non-POM changes.");
    }

    byte[] remoteData = null;
    try {
      remoteData = ByteStreams.toByteArray(remote);
    } catch (IOException e) {
      throw new ScmException(ScmOperation.MERGE, "Unable to read remote content!", e);
    } finally {
      // Closeables.closeQuietly(remote);
      try {
        Closeables.close(remote, true);
      } catch (IOException e) {
      }
    }

    Optional<Model> remoteModel = loadModel(new ByteArrayInputStream(remoteData));
    Optional<Model> baseModel = loadModel(base);

    Model resultModel = loadModel(new ByteArrayInputStream(remoteData)).get();
    mergeVersions(localModel.get(), remoteModel.get(), baseModel.get(), resultModel);
    mergeParentVersions(localModel.get(), remoteModel.get(), baseModel.get(), resultModel);

    try {
      Document document = PomUtil.parsePOM(new ByteArrayInputStream(remoteData));
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
          result.setVersion(remote.getVersion());
          // throw new ScmException(ScmOperation.MERGE,
          // "Could not merge POM version changes since the new remote version is higher than the local one!");
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

  private Optional<Model> loadModel(InputStream in) {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    try {
      Model model = reader.read(in);
      return Optional.of(model);
    } catch (Exception e) {
      return Optional.absent();
    } finally {
      // Closeables.closeQuietly(in);
      try {
        Closeables.close(in, true);
      } catch (IOException e) {
      }
    }
  }
}
