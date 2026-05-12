package me.whereareiam.configura.common.merge.strategy;

import me.whereareiam.configura.merge.strategy.MergeStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MergeStrategyFactory {
	private final Map<Class<? extends MergeStrategy>, MergeStrategy> cache = new ConcurrentHashMap<>();

	public MergeStrategy create(Class<? extends MergeStrategy> strategyClass) {
		return cache.computeIfAbsent(strategyClass, MergeStrategyFactory::instantiate);
	}

	private static MergeStrategy instantiate(Class<? extends MergeStrategy> strategyClass) {
		try {
			return strategyClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Merge strategy class must have an accessible no-arg constructor: " + strategyClass.getName(), e);
		}
	}
}
