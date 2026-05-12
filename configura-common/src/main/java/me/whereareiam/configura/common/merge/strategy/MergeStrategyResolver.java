package me.whereareiam.configura.common.merge.strategy;

import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.common.merge.MergeFieldResolver;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.type.DeepDefaults;

import java.lang.reflect.Field;

public final class MergeStrategyResolver {
	private final Class<? extends MergeStrategy> defaultStrategy;
	private final MergeStrategyRegistry strategyRegistry;

	public MergeStrategyResolver(Class<? extends MergeStrategy> defaultStrategy, MergeStrategyRegistry strategyRegistry) {
		this.defaultStrategy = defaultStrategy != null ? defaultStrategy : DeepDefaults.class;
		this.strategyRegistry = strategyRegistry != null ? strategyRegistry.copy() : MergeStrategyRegistry.standard();
	}

	public Class<? extends MergeStrategy> resolve(Class<?> ownerType, String fieldName) {
		return resolve(MergeFieldResolver.resolveField(ownerType, fieldName));
	}

	public Class<? extends MergeStrategy> resolve(Field field) {
		if (field == null) return defaultStrategy;

		Merge merge = field.getAnnotation(Merge.class);
		if (merge == null) return defaultStrategy;

		if (!merge.named().isBlank()) {
			Class<? extends MergeStrategy> named = strategyRegistry.get(merge.named());
			if (named == null) {
				throw new ConfigException("Unknown merge strategy: " + merge.named() + " on field " + field.getDeclaringClass().getName() + "#" + field.getName());
			}
			return named;
		}

		return merge.value();
	}
}
