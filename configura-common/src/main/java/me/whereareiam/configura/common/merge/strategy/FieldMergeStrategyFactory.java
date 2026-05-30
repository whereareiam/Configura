package me.whereareiam.configura.common.merge.strategy;

import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FieldMergeStrategyFactory {
	private final Map<Class<? extends FieldMergeStrategy>, FieldMergeStrategy> cache = new ConcurrentHashMap<>();

	public @NotNull FieldMergeStrategy create(
			@NotNull Class<? extends FieldMergeStrategy> strategyClass
	) {
		return cache.computeIfAbsent(strategyClass, FieldMergeStrategyFactory::instantiate);
	}

	private static @NotNull FieldMergeStrategy instantiate(
			@NotNull Class<? extends FieldMergeStrategy> strategyClass
	) {
		try {
			var constructor = strategyClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Merge strategy class must have an accessible no-arg constructor: " + strategyClass.getName(), e);
		}
	}
}
