package me.whereareiam.configura.merge;

import me.whereareiam.configura.type.PrimitiveDefaultPolicy;

/**
 * Policy describing how Configura should materialize defaults before merge.
 *
 * @param primitiveDefaultPolicy defaults materialization behavior
 */
public record MergePolicy(PrimitiveDefaultPolicy primitiveDefaultPolicy) {
	/**
	 * Policy used by update-like flows that build defaults from a synthetic empty model.
	 *
	 * @return update policy
	 */
	public static MergePolicy update() {
		return new MergePolicy(PrimitiveDefaultPolicy.AS_MISSING);
	}

	/**
	 * Policy used by save-like flows that should preserve all explicit model values.
	 *
	 * @return save policy
	 */
	public static MergePolicy save() {
		return new MergePolicy(PrimitiveDefaultPolicy.PRESERVE);
	}
}
