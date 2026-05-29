package me.whereareiam.configura.type.merge.tree.map;

/**
 * Controls how default map entries are materialized when source data is present.
 */
public enum MapPresence {
	DECLARED_ONLY,
	SEED_DEFAULTS,
	DEFAULT_DOMAIN_ONLY
}
