package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.DefaultsProviderRegistry;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public final class ClassProviderDefaultsResolver {
	private final DefaultsProviderRegistry defaultsRegistry;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public @Nullable JsonNode resolve(ObjectMapper mapper, Class<?> targetType) {
		if (!hasDefaults(targetType)) return null;

		Object instance = null;

		for (Class<?> type : hierarchy(targetType)) {
			DefaultsProvider<?> provider = defaultsRegistry == null ? null : defaultsRegistry.getProvider((Class) type);
			if (provider != null) {
				if (instance == null) instance = instantiate(targetType);
				Object supplied = ((DefaultsProvider) provider).supply(instance);
				if (supplied != null) instance = supplied;
			}

			DefaultsProvider<?> annotationProvider = annotationProvider(type);
			if (annotationProvider != null) {
				if (instance == null) instance = instantiate(targetType);
				Object supplied = ((DefaultsProvider) annotationProvider).supply(instance);
				if (supplied != null) instance = supplied;
			}
		}

		return instance == null ? null : mapper.valueToTree(instance);
	}

	@SuppressWarnings("rawtypes")
	public boolean hasDefaults(Class<?> targetType) {
		for (Class<?> type : hierarchy(targetType)) {
			DefaultsProvider<?> provider = defaultsRegistry == null ? null : defaultsRegistry.getProvider((Class) type);
			if (provider != null) return true;
			if (hasAnnotationProvider(type)) return true;
		}

		return false;
	}

	private static DefaultsProvider<?> annotationProvider(Class<?> targetType) {
		Defaults defaults = targetType.getAnnotation(Defaults.class);
		if (defaults == null || defaults.provider().value() == Defaults.Provider.None.class)
			return annotationProviderModern(targetType);
		return instantiateProvider(defaults.provider().value());
	}

	private static DefaultsProvider<?> annotationProviderModern(Class<?> targetType) {
		me.whereareiam.configura.annotation.merge.DefaultsProvider defaults =
				targetType.getAnnotation(me.whereareiam.configura.annotation.merge.DefaultsProvider.class);
		if (defaults == null) return null;
		return instantiateProvider(defaults.value());
	}

	private static boolean hasAnnotationProvider(Class<?> targetType) {
		Defaults defaults = targetType.getAnnotation(Defaults.class);
		return (defaults != null && defaults.provider().value() != Defaults.Provider.None.class)
				|| targetType.isAnnotationPresent(me.whereareiam.configura.annotation.merge.DefaultsProvider.class);
	}

	private static DefaultsProvider<?> instantiateProvider(Class<? extends DefaultsProvider<?>> providerClass) {
		try {
			var constructor = providerClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + providerClass.getName(), e);
		}
	}

	private static Object instantiate(Class<?> targetType) {
		try {
			var constructor = targetType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), e);
		}
	}

	private static List<Class<?>> hierarchy(Class<?> targetType) {
		List<Class<?>> hierarchy = new ArrayList<>();
		for (Class<?> type = targetType; type != null && type != Object.class; type = type.getSuperclass())
			hierarchy.add(type);

		Collections.reverse(hierarchy);
		return hierarchy;
	}
}
