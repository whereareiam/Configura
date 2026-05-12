package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public final class ModelDefaultsResolver implements FieldDefaultsResolver {
	private final DefaultMergeDefaultsRegistry defaultsRegistry;

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		if (defaultsRegistry == null) return null;
		MergeDefaultsProvider provider = defaultsRegistry.getDefaultsProvider((Class) targetType);
		if (provider == null) return null;

		Object instance = instantiate(targetType);
		Object supplied = provider.supply(instance);
		return supplied == null
				? null
				: mapper.valueToTree(supplied);
	}

	private static Object instantiate(Class<?> targetType) {
		try {
			return targetType.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), e);
		}
	}
}
