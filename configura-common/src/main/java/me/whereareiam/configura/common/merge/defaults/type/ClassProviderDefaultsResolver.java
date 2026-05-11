package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.merge.MergeDefaultsProvider;

@RequiredArgsConstructor
public final class ClassProviderDefaultsResolver {
	private final DefaultMergeDefaultsRegistry defaultsRegistry;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType) {
		MergeDefaultsProvider<?> provider = defaultsRegistry == null ? null : defaultsRegistry.getDefaultsProvider((Class) targetType);
		if (provider == null) provider = annotationProvider(targetType);
		if (provider == null) return null;

		Object instance = instantiate(targetType);
		Object supplied = ((MergeDefaultsProvider) provider).supply(instance);

		return supplied == null
				? null
				: mapper.valueToTree(supplied);
	}

	private static MergeDefaultsProvider<?> annotationProvider(Class<?> targetType) {
		Defaults defaults = targetType.getAnnotation(Defaults.class);
		if (defaults == null || defaults.provider().value() == Defaults.Provider.None.class)
			return null;
		return instantiateProvider(defaults.provider().value());
	}

	private static MergeDefaultsProvider<?> instantiateProvider(Class<? extends MergeDefaultsProvider<?>> providerClass) {
		try {
			return providerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + providerClass.getName(), e);
		}
	}

	private static Object instantiate(Class<?> targetType) {
		try {
			return targetType.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), e);
		}
	}
}
