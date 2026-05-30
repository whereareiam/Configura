package me.whereareiam.configura.merge.strategy.capability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed capability container attached to a merge strategy definition.
 */
public final class StrategyCapabilitySet {
	private final Map<StrategyCapabilityKey<?>, Object> values = new LinkedHashMap<>();

	public @NotNull StrategyCapabilitySet copy() {
		StrategyCapabilitySet copy = new StrategyCapabilitySet();
		copy.values.putAll(values);
		return copy;
	}

	public <T> @NotNull StrategyCapabilitySet put(@NotNull StrategyCapabilityKey<T> key, @NotNull T value) {
		values.put(key, value);
		return this;
	}

	public boolean has(@NotNull StrategyCapabilityKey<?> key) {
		return values.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T get(@NotNull StrategyCapabilityKey<T> key) {
		return (T) values.get(key);
	}

	public @NotNull Map<StrategyCapabilityKey<?>, Object> asMap() {
		return Map.copyOf(values);
	}
}
