package me.whereareiam.configura.internal;

import java.util.ServiceLoader;

public final class ProviderResolver {
	public static <P> P loadFirst(Class<P> serviceClass) {
		ClassLoader apiClassLoader = serviceClass.getClassLoader();
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		P loaded = tryLoad(serviceClass, apiClassLoader);
		if (loaded != null) return loaded;

		if (contextClassLoader != apiClassLoader) {
			loaded = tryLoad(serviceClass, contextClassLoader);
			return loaded;
		}

		return null;
	}

	private static <P> P tryLoad(Class<P> serviceClass, ClassLoader classLoader) {
		if (classLoader == null) return null;
		try {
			for (P provider : ServiceLoader.load(serviceClass, classLoader))
				return provider;
		} catch (Throwable ignored) {
		}

		return null;
	}
}


