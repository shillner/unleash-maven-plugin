package de.itemis.maven.aether;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Optional;

public final class ArtifactResolver {

	private ArtifactResolver() {}

	public static Optional<File> resolveArtifact(String groupId, String artifactId, String version,
		Optional<String> type,
		Optional<String> classifier, Log log, String updatePolicy) {
		Optional<File> result;

		RepositorySystem system = AetherBootstrap.newRepositorySystem();
		// TODO remove hard coded path to repository and get the one from settings or default to user.home ...

		final String defaultLocalRepositoryPath = System.getProperty("user.home") + "/.m2/repository";
		final String localRepositoryPath = System.getProperty("maven.repo.local", defaultLocalRepositoryPath);

		log.info("ArtifactResolver is using '" + localRepositoryPath + "' as localRepositoryPath");

		DefaultRepositorySystemSession session = AetherBootstrap.newRepositorySystemSession(system, localRepositoryPath);
		session.setUpdatePolicy(updatePolicy);
		log.debug("Update policy: " + session.getUpdatePolicy());
		
		Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier.orNull(), type.orNull(), version);

		// FIXME could not find artifact in central repo (if artifact is local only)
		ArtifactRequest artifactRequest = new ArtifactRequest();
		artifactRequest.setArtifact(artifact);
		artifactRequest.setRepositories(AetherBootstrap.newRemoteRepositories(system, session));

		try {
			// TODO maybe use DefaultArtifactResolver of the eclipse aether package?!
			ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
			artifact = artifactResult.getArtifact();
			if (artifact != null) {
				result = Optional.fromNullable(artifact.getFile());
			} else {
				result = Optional.absent();
			}
		} catch (ArtifactResolutionException e) {
			log.error("ArtifactResolver caught ArtifactResolutionException: " + e.getMessage(), e);
			// TODO add error handling -> maybe throw an exception that indicates the error or return an Optional
			result = Optional.absent();
		}

		return result;
	}
}
