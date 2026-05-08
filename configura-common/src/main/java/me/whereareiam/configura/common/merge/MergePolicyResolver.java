package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.merge.MergePolicyRegistry;
import me.whereareiam.configura.type.MergePreset;

import java.lang.reflect.Field;

public final class MergePolicyResolver {
	private final MergePolicy defaultPolicy;
	private final MergePolicyRegistry policyRegistry;

	public MergePolicyResolver() {
		this(MergePreset.DEEP_DEFAULTS.policy(), MergePolicyRegistry.standard());
	}

	public MergePolicyResolver(MergePolicy defaultPolicy, MergePolicyRegistry policyRegistry) {
		this.defaultPolicy = defaultPolicy != null ? defaultPolicy : MergePreset.DEEP_DEFAULTS.policy();
		this.policyRegistry = policyRegistry != null ? policyRegistry.copy() : MergePolicyRegistry.standard();
	}

	public MergePolicy resolve(Field field) {
		if (field == null) return defaultPolicy;

		Merge merge = field.getAnnotation(Merge.class);
		if (merge == null) return defaultPolicy;

		if (!merge.policy().isBlank()) {
			MergePolicy named = policyRegistry.get(merge.policy());
			if (named == null) {
				throw new ConfigException("Unknown merge policy: " + merge.policy() + " on field " + field.getDeclaringClass().getName() + "#" + field.getName());
			}

			return named;
		}

		return merge.preset().policy();
	}

	public MergePolicy resolve(Class<?> modelClass, String fieldName) {
		Field field = resolveField(modelClass, fieldName);
		return field != null ? resolve(field) : defaultPolicy;
	}

	private Field resolveField(Class<?> modelClass, String fieldName) {
		for (Field field : modelClass.getDeclaredFields()) {
			if (field.getName().equals(fieldName)) return field;
			if (resolveFieldName(field).equals(fieldName)) return field;
		}

		return null;
	}

	public static String resolveFieldName(Field field) {
		JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
		if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank())
			return jsonProperty.value();

		return field.getName();
	}
}
