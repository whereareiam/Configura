package me.whereareiam.configura.common.template;

import me.whereareiam.configura.TemplateProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateRegistry {
	private static final Map<Class<?>, TemplateProvider<?>> MODEL_SUPPLIERS = new ConcurrentHashMap<>();

	public static <T> void registerModel(Class<T> model, TemplateProvider<T> supplier) {
		MODEL_SUPPLIERS.put(model, supplier);
	}

	@SuppressWarnings("unchecked")
	public static <T> TemplateProvider<T> getModelProvider(Class<T> model) {
		return (TemplateProvider<T>) MODEL_SUPPLIERS.get(model);
	}
}


