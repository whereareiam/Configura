package me.whereareiam.configura.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

/**
 * Context object passed to config migration steps.
 */
@SuppressWarnings("unused")
public final class ConfigMigrationContext {
	private final Class<?> type;
	private final Path sourcePath;
	private final ObjectMapper mapper;
	private final int sourceVersion;
	private final int targetVersion;
	private final String versionField;

	public ConfigMigrationContext(
			Class<?> type,
			Path sourcePath,
			ObjectMapper mapper,
			int sourceVersion,
			int targetVersion,
			String versionField
	) {
		this.type = type;
		this.sourcePath = sourcePath;
		this.mapper = mapper;
		this.sourceVersion = sourceVersion;
		this.targetVersion = targetVersion;
		this.versionField = versionField;
	}

	public Class<?> type() {
		return type;
	}

	public Path sourcePath() {
		return sourcePath;
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	public int sourceVersion() {
		return sourceVersion;
	}

	public int targetVersion() {
		return targetVersion;
	}

	public String versionField() {
		return versionField;
	}

	public ObjectNode object(ObjectNode parent, String fieldName) {
		if (parent == null) throw new IllegalArgumentException("parent must not be null");
		if (fieldName == null || fieldName.isBlank())
			throw new IllegalArgumentException("fieldName must not be blank");

		if (parent.get(fieldName) instanceof ObjectNode objectNode)
			return objectNode;

		ObjectNode created = mapper.createObjectNode();
		parent.set(fieldName, created);
		return created;
	}
}
