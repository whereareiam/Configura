package me.whereareiam.configura.migration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder-style definition for a versioned root config type.
 *
 * @param <T> root configuration type
 */
public final class MigrationDefinition<T> {
	private final Class<T> type;
	private int currentVersion;
	private int assumeVersionWhenMissing;
	private String defaultVersionProperty = "_version";

	private final LinkedHashMap<Integer, ConfigMigrationStep<T>> migrations = new LinkedHashMap<>();

	public MigrationDefinition(Class<T> type) {
		if (type == null) throw new IllegalArgumentException("type must not be null");
		this.type = type;
	}

	public Class<T> type() {
		return type;
	}

	public int currentVersion() {
		return currentVersion;
	}

	public MigrationDefinition<T> currentVersion(int currentVersion) {
		if (currentVersion < 0)
			throw new IllegalArgumentException("currentVersion must be non-negative");
		this.currentVersion = currentVersion;
		return this;
	}

	public String versionField() {
		return defaultVersionProperty;
	}

	public MigrationDefinition<T> versionField(String versionField) {
		if (versionField == null || versionField.isBlank()) {
			throw new IllegalArgumentException("versionField must not be blank");
		}

		this.defaultVersionProperty = versionField;
		return this;
	}

	public int assumeVersionWhenMissing() {
		return assumeVersionWhenMissing;
	}

	public MigrationDefinition<T> assumeVersionWhenMissing(int assumeVersionWhenMissing) {
		if (assumeVersionWhenMissing < 0) throw new IllegalArgumentException("assumeVersionWhenMissing must be non-negative");
		this.assumeVersionWhenMissing = assumeVersionWhenMissing;
		return this;
	}

	public MigrationDefinition<T> migration(ConfigMigrationStep<T> step) {
		if (step == null) throw new IllegalArgumentException("step must not be null");
		if (!type.equals(step.type()))
			throw new IllegalArgumentException("Migration step type must match " + type.getName());
		if (step.fromVersion() < 0)
			throw new IllegalArgumentException("Migration fromVersion must be non-negative");
		if (step.toVersion() <= step.fromVersion())
			throw new IllegalArgumentException("Migration toVersion must be greater than fromVersion");
		if (migrations.containsKey(step.fromVersion()))
			throw new IllegalArgumentException("Duplicate migration from version " + step.fromVersion());

		migrations.put(step.fromVersion(), step);
		return this;
	}

	@SafeVarargs
	public final MigrationDefinition<T> migrations(ConfigMigrationStep<T>... steps) {
		if (steps == null) return this;
		for (ConfigMigrationStep<T> step : steps) migration(step);

		return this;
	}

	public Map<Integer, ConfigMigrationStep<T>> migrations() {
		return Map.copyOf(migrations);
	}

	public MigrationDefinition<T> copy() {
		MigrationDefinition<T> copy = new MigrationDefinition<>(type);
		copy.currentVersion = currentVersion;
		copy.defaultVersionProperty = defaultVersionProperty;
		copy.assumeVersionWhenMissing = assumeVersionWhenMissing;
		copy.migrations.putAll(migrations);
		return copy;
	}
}
