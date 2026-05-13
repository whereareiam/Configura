package me.whereareiam.configura.merge.strategy;

import lombok.Getter;
import me.whereareiam.configura.merge.strategy.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for named merge strategies.
 */
@Getter
public final class MergeStrategyRegistry {
	private final Map<String, Class<? extends MergeStrategy>> strategies = new LinkedHashMap<>();

	/**
	 * Creates a registry containing the built-in strategy names.
	 *
	 * @return standard strategy registry
	 */
	public static @NotNull MergeStrategyRegistry standard() {
		return new MergeStrategyRegistry().withBuiltIns();
	}

	/**
	 * Creates a copy of this registry.
	 *
	 * @return independent registry with the same registrations
	 */
	public @NotNull MergeStrategyRegistry copy() {
		MergeStrategyRegistry copy = new MergeStrategyRegistry();
		copy.strategies.putAll(this.strategies);
		return copy;
	}

	/**
	 * Registers built-in strategy names.
	 *
	 * @return this registry
	 */
	public @NotNull MergeStrategyRegistry withBuiltIns() {
		register("deepDefaults", DeepDefaults.class);
		register("sourceOwnsField", SourceOwnsField.class);
		register("neverDefaults", NeverDefaults.class);
		register("defaultKeysOnlyMap", DefaultKeysOnlyMap.class);
		register("declaredKeysOnlyMap", DeclaredKeysOnlyMap.class);
		register("structuralObject", StructuralObject.class);
		return this;
	}

	/**
	 * Registers a named strategy class.
	 *
	 * @param name strategy name used by {@code @Merge(named = "...")}
	 * @param strategy strategy implementation class
	 * @return this registry
	 */
	public @NotNull MergeStrategyRegistry register(@NotNull String name, @NotNull Class<? extends MergeStrategy> strategy) {
		if (name.isBlank())
			throw new IllegalArgumentException("Merge strategy name must not be blank");
		strategies.put(name, strategy);
		return this;
	}

	/**
	 * Resolves a named strategy class.
	 *
	 * @param name strategy name
	 * @return strategy class or {@code null} when not registered
	 */
	public @Nullable Class<? extends MergeStrategy> get(@Nullable String name) {
		return name == null ? null : strategies.get(name);
	}

	/**
	 * Returns an immutable snapshot of registered strategy classes.
	 *
	 * @return registered strategies
	 */
	public @NotNull Map<String, Class<? extends MergeStrategy>> asMap() {
		return Map.copyOf(strategies);
	}
}
