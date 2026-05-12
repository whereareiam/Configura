package me.whereareiam.configura.type;

/**
 * Controls how Configura materializes defaults from a Java model before merge.
 */
public enum PrimitiveDefaultPolicy {
	/**
	 * Treats Java primitive defaults such as {@code false} and {@code 0} as unset.
	 */
	AS_MISSING,
	/**
	 * Preserves the model values as-is during defaults materialization.
	 */
	PRESERVE
}
