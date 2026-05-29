package me.whereareiam.configura.type.merge.tree.list;

/**
 * Controls how default list entries are materialized when source data is present.
 */
public enum ListPresence {
	DECLARED_ONLY,
	SEED_DEFAULTS,
	DEFAULT_DOMAIN_ONLY
}
