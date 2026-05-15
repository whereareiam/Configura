package me.whereareiam.configura.type;

/**
 * Controls how Configura handles keys present in source data but missing from the current model.
 */
public enum UnknownFieldPolicy {
	/**
	 * Drops unknown source keys during merge/update.
	 */
	DROP,
	/**
	 * Keeps unknown source keys during merge/update.
	 */
	PRESERVE
}
