package me.whereareiam.configura.common.merge.defaults;

import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MergeDefaultsProviderRegistry {
	private final Map<Class<?>, MergeDefaultsProvider<?>> providers = new ConcurrentHashMap<>();

	public MergeDefaultsProviderRegistry copy() {
		MergeDefaultsProviderRegistry copy = new MergeDefaultsProviderRegistry();
		copy.providers.putAll(this.providers);
		return copy;
	}

	public <T, P extends MergeDefaultsProvider<T>> void registerProvider(Class<P> providerClass) {
		MergeDefaultsProvider<T> provider;
		try {
			provider = providerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Defaults provider class must have an accessible no-arg constructor: " + providerClass.getName(), e);
		}

		Class<?> modelType = resolveModelType(provider);
		if (modelType == null)
			throw new IllegalArgumentException("Unable to resolve model type for defaults provider: " + providerClass.getName());

		providers.put(modelType, provider);
	}

	@SuppressWarnings("unchecked")
	public <T> MergeDefaultsProvider<T> getProvider(Class<T> modelType) {
		return (MergeDefaultsProvider<T>) providers.get(modelType);
	}

	private static Class<?> resolveModelType(MergeDefaultsProvider<?> provider) {
		for (Type type : provider.getClass().getGenericInterfaces()) {
			if (type instanceof ParameterizedType pt && pt.getRawType() == MergeDefaultsProvider.class) {
				Type arg = pt.getActualTypeArguments()[0];
				if (arg instanceof Class) return (Class<?>) arg;
			}
		}

		return null;
	}
}
