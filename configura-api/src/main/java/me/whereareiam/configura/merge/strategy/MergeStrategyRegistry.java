package me.whereareiam.configura.merge.strategy;

import me.whereareiam.configura.exception.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of strategy definitions keyed by class and optional aliases.
 */
public final class MergeStrategyRegistry {
	private final Map<Class<? extends FieldMergeStrategy>, MergeStrategyDefinition> byClass = new LinkedHashMap<>();
	private final Map<String, MergeStrategyDefinition> byAlias = new LinkedHashMap<>();

	public @NotNull MergeStrategyRegistry copy() {
		MergeStrategyRegistry copy = new MergeStrategyRegistry();
		copy.byClass.putAll(byClass);
		copy.byAlias.putAll(byAlias);
		return copy;
	}

	public @NotNull MergeStrategyRegistry register(@NotNull MergeStrategyDefinition definition) {
		MergeStrategyDefinition previous = byClass.put(definition.getStrategyClass(), definition);
		if (previous != null) {
			for (String alias : previous.getAliases())
				byAlias.remove(alias, previous);
		}
		for (String alias : definition.getAliases()) {
			MergeStrategyDefinition existing = byAlias.get(alias);
			if (existing != null && existing.getStrategyClass() != definition.getStrategyClass())
				throw new ConfigException("Merge strategy alias '" + alias + "' is already registered for " + existing.getStrategyClass().getName());
			byAlias.put(alias, definition);
		}
		return this;
	}

	public @NotNull MergeStrategyRegistry registerAlias(
			@NotNull String alias,
			@NotNull Class<? extends FieldMergeStrategy> strategyClass
	) {
		MergeStrategyDefinition existing = byClass.get(strategyClass);
		if (existing == null)
			return register(MergeStrategyDefinition.builder(strategyClass).alias(alias).build());

		MergeStrategyDefinition.Builder builder = MergeStrategyDefinition.builder(strategyClass);
		for (String existingAlias : existing.getAliases())
			builder.alias(existingAlias);
		for (var entry : existing.getCapabilities().asMap().entrySet())
			putCapability(builder, entry.getKey(), entry.getValue());
		builder.alias(alias);
		return register(builder.build());
	}

	public @Nullable MergeStrategyDefinition definition(@NotNull Class<? extends FieldMergeStrategy> strategyClass) {
		return byClass.get(strategyClass);
	}

	public @Nullable MergeStrategyDefinition definition(@Nullable String alias) {
		return alias == null ? null : byAlias.get(alias);
	}

	public @NotNull MergeStrategyDefinition definitionOrAdHoc(@NotNull Class<? extends FieldMergeStrategy> strategyClass) {
		MergeStrategyDefinition definition = definition(strategyClass);
		return definition != null
				? definition
				: MergeStrategyDefinition.builder(strategyClass).build();
	}

	public @NotNull Map<String, Class<? extends FieldMergeStrategy>> aliases() {
		Map<String, Class<? extends FieldMergeStrategy>> aliases = new LinkedHashMap<>();
		byAlias.forEach((alias, definition) -> aliases.put(alias, definition.getStrategyClass()));
		return Map.copyOf(aliases);
	}

	@SuppressWarnings("unchecked")
	private static <T> void putCapability(
			MergeStrategyDefinition.Builder builder,
			me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey<?> key,
			Object value
	) {
		builder.capability((me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey<T>) key, (T) value);
	}
}
