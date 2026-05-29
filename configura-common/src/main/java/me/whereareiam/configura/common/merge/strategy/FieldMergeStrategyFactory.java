package me.whereareiam.configura.common.merge.strategy;

import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FieldMergeStrategyFactory {
	private final Map<Class<? extends FieldMergeStrategy>, FieldMergeStrategy> cache = new ConcurrentHashMap<>();

	public FieldMergeStrategy create(Class<? extends FieldMergeStrategy> strategyClass) {
		return cache.computeIfAbsent(strategyClass, FieldMergeStrategyFactory::instantiate);
	}

	private static FieldMergeStrategy instantiate(Class<? extends FieldMergeStrategy> strategyClass) {
		try {
			return strategyClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Merge strategy class must have an accessible no-arg constructor: " + strategyClass.getName(), e);
		}
	}
}
