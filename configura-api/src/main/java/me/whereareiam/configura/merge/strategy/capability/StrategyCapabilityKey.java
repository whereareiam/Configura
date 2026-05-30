package me.whereareiam.configura.merge.strategy.capability;

import org.jetbrains.annotations.NotNull;

/**
 * Typed key used to store strategy capabilities.
 *
 * @param <T> capability value type
 */
public final class StrategyCapabilityKey<T> {
	private final String name;

	private StrategyCapabilityKey(String name) {
		this.name = name;
	}

	public static <T> @NotNull StrategyCapabilityKey<T> of(@NotNull String name) {
		return new StrategyCapabilityKey<>(name);
	}

	public @NotNull String getName() {
		return name;
	}
}
