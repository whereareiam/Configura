package me.whereareiam.configura.feature.extension.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of extension rules.
 */
public final class ConfigDocumentRuleRegistry {
	private final Map<Class<?>, ConfigDocumentRule<?>> wholeRules = new LinkedHashMap<>();
	private final Map<Class<?>, List<ConfigDocumentRule<?>>> contextualRules = new LinkedHashMap<>();

	/**
	 * Returns a copy of this registry.
	 *
	 * @return copied registry
	 */
	public @NotNull ConfigDocumentRuleRegistry copy() {
		ConfigDocumentRuleRegistry copy = new ConfigDocumentRuleRegistry();
		copy.wholeRules.putAll(wholeRules);
		contextualRules.forEach((type, rules) -> copy.contextualRules.put(type, new ArrayList<>(rules)));
		return copy;
	}

	/**
	 * Registers one document rule.
	 *
	 * @param rule rule to register
	 * @return this registry
	 */
	public @NotNull ConfigDocumentRuleRegistry register(@NotNull ConfigDocumentRule<?> rule) {
		if (rule.isWholeDocument()) {
			ConfigDocumentRule<?> previous = wholeRules.putIfAbsent(rule.getBaseType(), rule);
			if (previous != null)
				throw new IllegalArgumentException("A whole-document rule is already registered for " + rule.getBaseType().getName());
			return this;
		}

		contextualRules.computeIfAbsent(rule.getBaseType(), ignored -> new ArrayList<>()).add(rule);
		return this;
	}

	/**
	 * Returns whether any rules are registered for the given base type.
	 *
	 * @param baseType declared base type
	 * @return {@code true} when at least one rule exists
	 */
	public boolean hasRules(@NotNull Class<?> baseType) {
		return wholeRules.containsKey(baseType) || contextualRules.containsKey(baseType);
	}

	/**
	 * Returns the whole-document rule registered for the given base type.
	 *
	 * @param baseType declared base type
	 * @return registered whole-document rule, or {@code null} when absent
	 */
	public ConfigDocumentRule<?> getWholeRule(@NotNull Class<?> baseType) {
		return wholeRules.get(baseType);
	}

	/**
	 * Returns contextual rules registered for the given base type.
	 *
	 * @param baseType declared base type
	 * @return immutable list of contextual rules
	 */
	public @NotNull List<ConfigDocumentRule<?>> getContextualRules(@NotNull Class<?> baseType) {
		List<ConfigDocumentRule<?>> rules = contextualRules.get(baseType);
		return rules != null ? List.copyOf(rules) : List.of();
	}
}
