package me.whereareiam.configura.common.template;

import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.template.TemplateRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTemplateRegistry implements TemplateRegistry {
	private static final Map<Class<?>, TemplateProvider<?>> TEMPLATE_SUPPLIERS = new ConcurrentHashMap<>();

	@Override
	public <T, P extends TemplateProvider<T>> void registerTemplate(Class<P> providerClass) {
		TemplateProvider<T> provider;
		try {
			provider = providerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Provider class must have an accessible no-arg constructor: " + providerClass.getName(), e);
		}

		Class<?> modelType = resolveModelType(provider);
		if (modelType == null) {
			throw new IllegalArgumentException("Unable to resolve model type for provider: " + providerClass.getName());
		}
		TEMPLATE_SUPPLIERS.put(modelType, provider);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> TemplateProvider<T> getTemplateProvider(Class<T> modelType) {
		return (TemplateProvider<T>) TEMPLATE_SUPPLIERS.get(modelType);
	}

	private static Class<?> resolveModelType(TemplateProvider<?> provider) {
		for (Type type : provider.getClass().getGenericInterfaces()) {
			if (type instanceof ParameterizedType pt) {
				if (pt.getRawType() == TemplateProvider.class) {
					Type arg = pt.getActualTypeArguments()[0];
					if (arg instanceof Class) return (Class<?>) arg;
				}
			}
		}

		return null;
	}
}