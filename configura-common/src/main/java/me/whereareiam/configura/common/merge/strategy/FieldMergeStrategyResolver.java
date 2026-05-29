package me.whereareiam.configura.common.merge.strategy;

import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import org.jetbrains.annotations.Nullable;

public final class FieldMergeStrategyResolver {
	private final @Nullable Class<? extends FieldMergeStrategy> defaultStrategy;
	private final FieldMergeStrategyRegistry strategyRegistry;

	public FieldMergeStrategyResolver(
			@Nullable Class<? extends FieldMergeStrategy> defaultStrategy,
			FieldMergeStrategyRegistry strategyRegistry
	) {
		this.defaultStrategy = defaultStrategy;
		this.strategyRegistry = strategyRegistry != null ? strategyRegistry.copy() : FieldMergeStrategyRegistry.standard();
	}

	public @Nullable Class<? extends FieldMergeStrategy> resolve(MergePolicy policy, String fieldDescription) {
		if (policy.getNamedStrategy() != null) {
			Class<? extends FieldMergeStrategy> named = strategyRegistry.get(policy.getNamedStrategy());
			if (named == null) throw new ConfigException("Unknown merge strategy: " + policy.getNamedStrategy() + " on field " + fieldDescription);
			return named;
		}
		if (policy.getStrategyClass() != null) return policy.getStrategyClass();
		return defaultStrategy;
	}
}
