package me.whereareiam.configura.merge;

import me.whereareiam.configura.type.MergePreset;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MergePolicyRegistry {
	private final Map<String, MergePolicy> policies = new LinkedHashMap<>();

	public static MergePolicyRegistry standard() {
		return new MergePolicyRegistry().withBuiltIns();
	}

	public MergePolicyRegistry copy() {
		MergePolicyRegistry copy = new MergePolicyRegistry();
		copy.policies.putAll(this.policies);
		return copy;
	}

	public MergePolicyRegistry withBuiltIns() {
		for (MergePreset preset : MergePreset.values())
			register(preset.name(), preset.policy());
		return this;
	}

	public MergePolicyRegistry register(String name, MergePolicy policy) {
		if (name == null || name.isBlank())
			throw new IllegalArgumentException("Merge policy name must not be blank");
		if (policy == null)
			throw new IllegalArgumentException("Merge policy must not be null");

		policies.put(name, policy);
		return this;
	}

	public MergePolicy get(String name) {
		return policies.get(name);
	}

	public Map<String, MergePolicy> asMap() {
		return new LinkedHashMap<>(policies);
	}
}
