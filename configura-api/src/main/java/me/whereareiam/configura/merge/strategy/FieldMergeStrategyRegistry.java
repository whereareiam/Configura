package me.whereareiam.configura.merge.strategy;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for named merge strategies.
 */
@Getter
public final class FieldMergeStrategyRegistry {
	private final Map<String, Class<? extends FieldMergeStrategy>> strategies = new LinkedHashMap<>();

	/**
	 * Creates an empty strategy registry.
	 *
	 * @return empty strategy registry
	 */
	public static @NotNull FieldMergeStrategyRegistry standard() {
		return new FieldMergeStrategyRegistry();
	}

	/**
	 * Creates a copy of this registry.
	 *
	 * @return independent registry with the same registrations
	 */
	public @NotNull FieldMergeStrategyRegistry copy() {
		FieldMergeStrategyRegistry copy = new FieldMergeStrategyRegistry();
		copy.strategies.putAll(this.strategies);
		return copy;
	}

	/**
	 * Registers a named strategy class.
	 *
	 * @param name strategy name used by {@code @Merge(named = "...")}
	 * @param strategy strategy implementation class
	 * @return this registry
	 */
	public @NotNull FieldMergeStrategyRegistry register(@NotNull String name, @NotNull Class<? extends FieldMergeStrategy> strategy) {
		if (name.isBlank()) throw new IllegalArgumentException("Merge strategy name must not be blank");
		strategies.put(name, strategy);
		return this;
	}

	/**
	 * Resolves a named strategy class.
	 *
	 * @param name strategy name
	 * @return strategy class or {@code null} when not registered
	 */
	public @Nullable Class<? extends FieldMergeStrategy> get(@Nullable String name) {
		return name == null ? null : strategies.get(name);
	}

	/**
	 * Returns an immutable snapshot of registered strategy classes.
	 *
	 * @return registered strategies
	 */
	public @NotNull Map<String, Class<? extends FieldMergeStrategy>> asMap() {
		return Map.copyOf(strategies);
	}
}
