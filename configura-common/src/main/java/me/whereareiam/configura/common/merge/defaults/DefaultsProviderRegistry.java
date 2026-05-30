package me.whereareiam.configura.common.merge.defaults;

import me.whereareiam.configura.merge.defaults.DefaultsProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultsProviderRegistry {
	private final Map<Class<?>, DefaultsProvider<?>> providers = new ConcurrentHashMap<>();

	public DefaultsProviderRegistry copy() {
		DefaultsProviderRegistry copy = new DefaultsProviderRegistry();
		copy.providers.putAll(this.providers);
		return copy;
	}

	public <T, P extends DefaultsProvider<T>> void registerProvider(Class<P> providerClass) {
		DefaultsProvider<T> provider;
		try {
			var constructor = providerClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			provider = constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Defaults provider class must have an accessible no-arg constructor: " + providerClass.getName(), e);
		}

		Class<?> modelType = resolveModelType(provider);
		if (modelType == null)
			throw new IllegalArgumentException("Unable to resolve model type for defaults provider: " + providerClass.getName());

		providers.put(modelType, provider);
	}

	@SuppressWarnings("unchecked")
	public <T> DefaultsProvider<T> getProvider(Class<T> modelType) {
		return (DefaultsProvider<T>) providers.get(modelType);
	}

	private static Class<?> resolveModelType(DefaultsProvider<?> provider) {
		for (Type type : provider.getClass().getGenericInterfaces()) {
			if (type instanceof ParameterizedType pt && pt.getRawType() == DefaultsProvider.class) {
				Type arg = pt.getActualTypeArguments()[0];
				if (arg instanceof Class) return (Class<?>) arg;
			}
		}

		return null;
	}
}
