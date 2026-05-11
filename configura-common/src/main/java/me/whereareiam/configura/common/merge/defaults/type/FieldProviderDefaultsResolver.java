package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;
import me.whereareiam.configura.merge.MergeDefaultsProvider;

import java.lang.reflect.Field;

public final class FieldProviderDefaultsResolver implements FieldDefaultsResolver {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null || defaults.provider().value() == Defaults.Provider.None.class) return null;

		MergeDefaultsProvider provider = instantiate(defaults.provider().value());
		Object instance = instantiateTarget(targetType);
		Object supplied = provider.supply(instance);
		return supplied == null ? null : mapper.valueToTree(supplied);
	}

	private static MergeDefaultsProvider<?> instantiate(Class<? extends MergeDefaultsProvider<?>> providerClass) {
		try {
			return providerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + providerClass.getName(), e);
		}
	}

	private static Object instantiateTarget(Class<?> targetType) {
		try {
			return targetType.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), e);
		}
	}
}
