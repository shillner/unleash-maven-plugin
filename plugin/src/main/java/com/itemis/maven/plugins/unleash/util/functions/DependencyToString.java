package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Dependency;

import com.google.common.base.Function;

/**
 * A function to convert a {@link Dependency} to its String representation in coordinates format.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public enum DependencyToString implements Function<Dependency, String> {
	INSTANCE(true),
	NO_TYPE(false);

	private boolean includeType;

	private DependencyToString(boolean includeType) {
		this.includeType = includeType;
	}

	@Override
	public String apply(Dependency d) {
		StringBuilder sb = new StringBuilder(d.getGroupId());
		sb.append(":").append(d.getArtifactId());
		if (includeType && d.getType() != null) {
			sb.append(":").append(d.getType());
		}
		if (d.getClassifier() != null) {
			sb.append(":").append(d.getClassifier());
		}
		if (d.getVersion() != null) {
			sb.append(":").append(d.getVersion());
		}
		return sb.toString();
	}
}
