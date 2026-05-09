package me.whereareiam.configura.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A single forward migration step for one root configuration type.
 *
 * @param <T> root configuration type
 */
public interface ConfigMigrationStep<T> {
	Class<T> type();

	int fromVersion();

	int toVersion();

	ObjectNode migrate(ObjectNode root, ConfigMigrationContext context);
}
