package me.whereareiam.configura.merge.type;

import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey;

/**
 * Built-in typed capabilities consumed by Configura's own type adapters.
 */
public final class BuiltinStrategyCapabilities {
	public static final StrategyCapabilityKey<ListCapability> LIST = StrategyCapabilityKey.of("configura:list");
	public static final StrategyCapabilityKey<MapCapability> MAP = StrategyCapabilityKey.of("configura:map");

	public record ListCapability(Mode mode) {
		public enum Mode {
			DEEP_DEFAULTS,
			SOURCE_OWNS,
			NEVER_DEFAULTS
		}
	}

	public record MapCapability(Mode mode) {
		public enum Mode {
			DEEP_DEFAULTS,
			SOURCE_OWNS,
			NEVER_DEFAULTS
		}
	}
}
