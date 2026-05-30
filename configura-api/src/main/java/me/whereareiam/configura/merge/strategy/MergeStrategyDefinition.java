package me.whereareiam.configura.merge.strategy;

import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey;
import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilitySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Public definition of a merge strategy class, its aliases, and its typed capabilities.
 */
public final class MergeStrategyDefinition {
	private final Class<? extends FieldMergeStrategy> strategyClass;
	private final Set<String> aliases;
	private final StrategyCapabilitySet capabilities;

	private MergeStrategyDefinition(
			Class<? extends FieldMergeStrategy> strategyClass,
			Set<String> aliases,
			StrategyCapabilitySet capabilities
	) {
		this.strategyClass = strategyClass;
		this.aliases = Set.copyOf(aliases);
		this.capabilities = capabilities.copy();
	}

	public static @NotNull Builder builder(@NotNull Class<? extends FieldMergeStrategy> strategyClass) {
		return new Builder(strategyClass);
	}

	public @NotNull Class<? extends FieldMergeStrategy> getStrategyClass() {
		return strategyClass;
	}

	public @NotNull Set<String> getAliases() {
		return aliases;
	}

	public @NotNull StrategyCapabilitySet getCapabilities() {
		return capabilities.copy();
	}

	public boolean hasAlias(@Nullable String alias) {
		return alias != null && aliases.contains(alias);
	}

	public boolean hasCapability(@NotNull StrategyCapabilityKey<?> key) {
		return capabilities.has(key);
	}

	public <T> @Nullable T capability(@NotNull StrategyCapabilityKey<T> key) {
		return capabilities.get(key);
	}

	public static final class Builder {
		private final Class<? extends FieldMergeStrategy> strategyClass;
		private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
		private final StrategyCapabilitySet capabilities = new StrategyCapabilitySet();

		private Builder(Class<? extends FieldMergeStrategy> strategyClass) {
			this.strategyClass = strategyClass;
		}

		public @NotNull Builder alias(@NotNull String alias) {
			if (alias.isBlank()) throw new IllegalArgumentException("alias must not be blank");
			aliases.add(alias);
			return this;
		}

		public <T> @NotNull Builder capability(@NotNull StrategyCapabilityKey<T> key, @NotNull T value) {
			capabilities.put(key, value);
			return this;
		}

		public @NotNull MergeStrategyDefinition build() {
			return new MergeStrategyDefinition(strategyClass, aliases, capabilities);
		}
	}
}
