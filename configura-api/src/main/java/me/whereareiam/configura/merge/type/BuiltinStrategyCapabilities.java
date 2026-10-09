package me.whereareiam.configura.merge.type;

import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey;

/**
 * Capabilities the built-in type adapters look for on a merge strategy.
 */
public final class BuiltinStrategyCapabilities {
	/** How a strategy treats a list or map field as a whole; a strategy without it cannot be used on one. */
	public static final StrategyCapabilityKey<ContainerMode> CONTAINER = StrategyCapabilityKey.of("configura:container");

	private BuiltinStrategyCapabilities() {
	}

	/**
	 * What happens to a list or map field when file and defaults both have something to say.
	 */
	public enum ContainerMode {
		/** Entries are merged one by one, following the field's list or map policy. */
		DEEP_DEFAULTS,

		/** The file's value is taken as it is; the default is used only when the file has none. */
		SOURCE_OWNS,

		/** The file's value is taken as it is and the default is never used. */
		NEVER_DEFAULTS
	}
}
