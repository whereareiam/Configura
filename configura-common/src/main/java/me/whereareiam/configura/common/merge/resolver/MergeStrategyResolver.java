package me.whereareiam.configura.common.merge.resolver;

import me.whereareiam.configura.common.merge.strategy.FieldMergeStrategyFactory;
import me.whereareiam.configura.common.merge.strategy.ResolvedMergeStrategy;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyDefinition;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MergeStrategyResolver {
	private final @Nullable Class<? extends FieldMergeStrategy> defaultStrategyClass;
	private final @Nullable String defaultStrategyName;
	private final MergeStrategyRegistry strategyRegistry;
	private final FieldMergeStrategyFactory strategyFactory = new FieldMergeStrategyFactory();

	public MergeStrategyResolver(
			@Nullable Class<? extends FieldMergeStrategy> defaultStrategyClass,
			@Nullable String defaultStrategyName,
			@Nullable MergeStrategyRegistry strategyRegistry
	) {
		this.defaultStrategyClass = defaultStrategyClass;
		this.defaultStrategyName = defaultStrategyName;
		this.strategyRegistry = strategyRegistry != null ? strategyRegistry.copy() : new MergeStrategyRegistry();
	}

	public @NotNull ResolvedMergeStrategy resolve(@NotNull MergePolicy policy, @NotNull String fieldDescription) {
		if (policy.getNamedStrategy() != null) {
			MergeStrategyDefinition definition = strategyRegistry.definition(policy.getNamedStrategy());
			if (definition == null) throw new ConfigException("Unknown merge strategy: " + policy.getNamedStrategy() + " on field " + fieldDescription);
			return resolved(definition);
		}

		if (policy.getStrategyClass() != null) return resolved(strategyRegistry.definitionOrAdHoc(policy.getStrategyClass()));

		if (defaultStrategyName != null) {
			MergeStrategyDefinition definition = strategyRegistry.definition(defaultStrategyName);
			if (definition == null) throw new ConfigException("Unknown default merge strategy: " + defaultStrategyName + " on field " + fieldDescription);
			return resolved(definition);
		}

		if (defaultStrategyClass != null) return resolved(strategyRegistry.definitionOrAdHoc(defaultStrategyClass));

		throw new ConfigException("No default merge strategy configured for field " + fieldDescription);
	}

	private @NotNull ResolvedMergeStrategy resolved(@NotNull MergeStrategyDefinition definition) {
		return new ResolvedMergeStrategy(definition, strategyFactory.create(definition.getStrategyClass()));
	}
}
