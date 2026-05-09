package me.whereareiam.configura.common.migration;

import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.migration.MigrationDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MigrationDefinitionRegistry {
	private final Map<Class<?>, MigrationDefinition<?>> specs = new LinkedHashMap<>();

	public MigrationDefinitionRegistry copy() {
		MigrationDefinitionRegistry copy = new MigrationDefinitionRegistry();
		specs.forEach((type, spec) -> copy.specs.put(type, spec.copy()));
		return copy;
	}

	public <T> MigrationDefinitionRegistry register(MigrationDefinition<T> definition) {
		if (definition == null) throw new ConfigException("Versioned config spec must not be null");

		validate(definition);
		if (specs.containsKey(definition.type()))
			throw new ConfigException("Versioned config already registered for " + definition.type().getName());

		specs.put(definition.type(), definition.copy());
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> MigrationDefinition<T> get(Class<T> type) {
		return (MigrationDefinition<T>) specs.get(type);
	}

	public boolean contains(Class<?> type) {
		return specs.containsKey(type);
	}

	private <T> void validate(MigrationDefinition<T> definition) {
		if (definition.currentVersion() < 0)
			throw new ConfigException("Current version must be non-negative for " + definition.type().getName());
		if (definition.assumeVersionWhenMissing() < 0)
			throw new ConfigException("Missing version assumption must be non-negative for " + definition.type().getName());
		if (definition.versionField() == null || definition.versionField().isBlank())
			throw new ConfigException("Version field must not be blank for " + definition.type().getName());

		int cursor = definition.assumeVersionWhenMissing();
		if (cursor > definition.currentVersion())
			throw new ConfigException("Missing version assumption cannot exceed current version for " + definition.type().getName());

		while (cursor < definition.currentVersion()) {
			var step = definition.migrations().get(cursor);
			if (step == null) {
				throw new ConfigException(
						"Missing migration step for " + definition.type().getName() + " from version " + cursor
				);
			}
			if (step.toVersion() > definition.currentVersion()) {
				throw new ConfigException(
						"Migration step overshoots current version for " + definition.type().getName()
				);
			}

			cursor = step.toVersion();
		}
	}
}
