package me.whereareiam.configura.common.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.DocumentVersion;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.migration.ConfigMigrationContext;
import me.whereareiam.configura.migration.MigrationDefinition;

import java.lang.reflect.Field;
import java.nio.file.Path;

public final class SchemaMigrationEngine {
	private final ObjectMapper mapper;
	private final MigrationDefinitionRegistry registry;

	public SchemaMigrationEngine(ObjectMapper mapper, MigrationDefinitionRegistry registry) {
		this.mapper = mapper;
		this.registry = registry != null ? registry.copy() : new MigrationDefinitionRegistry();
	}

	public <T> MigrationResult migrate(Class<T> type, JsonNode source, Path sourcePath, boolean syntheticSource) {
		ObjectNode root = requireObjectRoot(source, type, syntheticSource);
		MigrationDefinition<T> definition = registry.get(type);
		if (definition == null) return new MigrationResult(root, new ResolvedVersionField(null, false), false);

		ResolvedVersionField resolvedVersionField = resolveVersionField(
				type,
				definition.versionField(),
				root.has(definition.versionField())
		);

		if (syntheticSource) {
			stampVersion(root, definition, resolvedVersionField);
			return new MigrationResult(root, resolvedVersionField, false);
		}

		int sourceVersion = readVersion(root, definition, type, resolvedVersionField);
		if (sourceVersion > definition.currentVersion()) {
			throw new ConfigException(
					"Config version " + sourceVersion + " is newer than supported version " + definition.currentVersion()
							+ " for " + type.getName()
			);
		}

		ObjectNode migrated = root;
		int cursor = sourceVersion;
		while (cursor < definition.currentVersion()) {
			var step = definition.migrations().get(cursor);
			if (step == null) {
				throw new ConfigException("Missing migration step for " + type.getName() + " from version " + cursor);
			}

			ConfigMigrationContext context = new ConfigMigrationContext(
					type,
					sourcePath,
					mapper,
					cursor,
					step.toVersion(),
					resolvedVersionField.fieldName() != null ? resolvedVersionField.fieldName() : definition.versionField()
			);

			ObjectNode next = step.migrate(migrated, context);
			if (next == null) {
				throw new ConfigException(
						"Migration step returned null for " + type.getName() + " from version " + cursor
				);
			}

			migrated = next;
			cursor = step.toVersion();
		}

		stampVersion(migrated, definition, resolvedVersionField);
		return new MigrationResult(migrated, resolvedVersionField, sourceVersion < definition.currentVersion());
	}

	public <T> ObjectNode stampCurrentVersion(Class<T> type, ObjectNode node) {
		MigrationDefinition<T> definition = registry.get(type);
		if (definition == null) return node;

		stampVersion(node, definition, resolveVersionField(type, definition.versionField(), node.has(definition.versionField())));
		return node;
	}

	public <T> ObjectNode stampCurrentVersion(Class<T> type, ObjectNode node, JsonNode source) {
		MigrationDefinition<T> definition = registry.get(type);
		if (definition == null) return node;

		ResolvedVersionField resolvedVersionField = resolveVersionField(
				type,
				definition.versionField(),
				source instanceof ObjectNode objectNode && objectNode.has(definition.versionField())
		);
		stampVersion(node, definition, resolvedVersionField);
		return node;
	}

	private <T> int readVersion(ObjectNode root, MigrationDefinition<T> definition, Class<T> type, ResolvedVersionField resolvedVersionField) {
		String fieldName = resolvedVersionField.fieldName() != null ? resolvedVersionField.fieldName() : definition.versionField();
		JsonNode versionNode = root.get(fieldName);

		if (versionNode == null || versionNode.isNull()) return definition.assumeVersionWhenMissing();
		if (!versionNode.canConvertToInt()) {
			throw new ConfigException(
					"Version field '" + fieldName + "' must be an integer for " + type.getName()
			);
		}

		int version = versionNode.intValue();
		if (version < 0) {
			throw new ConfigException(
					"Version field '" + fieldName + "' must be non-negative for " + type.getName()
			);
		}

		return version;
	}

	private <T> void stampVersion(ObjectNode root, MigrationDefinition<T> definition, ResolvedVersionField resolvedVersionField) {
		if (resolvedVersionField.fieldName() != null && !resolvedVersionField.fieldName().equals(definition.versionField()))
			root.remove(definition.versionField());
		if (!resolvedVersionField.persist())
			return;

		String fieldName = resolvedVersionField.fieldName();
		ObjectNode ordered = root.objectNode();
		ordered.put(fieldName, definition.currentVersion());
		for (var entry : root.properties()) {
			if (!fieldName.equals(entry.getKey()))
				ordered.set(entry.getKey(), entry.getValue());
		}

		root.removeAll();
		root.setAll(ordered);
	}

	private <T> ObjectNode requireObjectRoot(JsonNode source, Class<T> type, boolean syntheticSource) {
		if (source == null || source.isNull()) return mapper.createObjectNode();
		if (source instanceof ObjectNode objectNode) return objectNode.deepCopy();
		if (syntheticSource) return mapper.createObjectNode();

		throw new ConfigException("Config root must be an object for " + type.getName());
	}

	private ResolvedVersionField resolveVersionField(Class<?> type, String fallbackField, boolean sourceContainsFallback) {
		Field annotated = resolveAnnotatedField(type);
		if (annotated != null) return new ResolvedVersionField(SerializedFieldResolver.resolveSerializedName(annotated), true);

		Field matching = resolveMatchingSerializedField(type, fallbackField);
		if (matching != null) return new ResolvedVersionField(SerializedFieldResolver.resolveSerializedName(matching), true);
		if (sourceContainsFallback) return new ResolvedVersionField(fallbackField, true);

		return new ResolvedVersionField(fallbackField, false);
	}

	private Field resolveAnnotatedField(Class<?> type) {
		Field match = null;
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getAnnotation(DocumentVersion.class) == null) continue;
				if (match != null) throw new ConfigException("Multiple @DocumentVersion fields found for " + type.getName());
				match = field;
			}
		}
		return match;
	}

	private Field resolveMatchingSerializedField(Class<?> type, String fallbackField) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (fallbackField.equals(SerializedFieldResolver.resolveSerializedName(field)))
					return field;
			}
		}

		return null;
	}

	public record MigrationResult(ObjectNode node, ResolvedVersionField resolvedVersionField, boolean migrated) {
	}

	private record ResolvedVersionField(String fieldName, boolean persist) {
	}
}
