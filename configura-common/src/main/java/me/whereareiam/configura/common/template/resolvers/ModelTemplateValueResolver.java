package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.common.template.TemplateValueResolver;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;

import java.lang.reflect.Field;

public final class ModelTemplateValueResolver implements TemplateValueResolver {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		TemplateProvider<?> modelProvider = new DefaultTemplateRegistry().getTemplateProvider((Class) targetType);
		if (modelProvider == null) return null;

		Object instance = newInstance((Class) targetType);
		return ((TemplateProvider) modelProvider).supply(instance);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object newInstance(Class targetType) {
		try {
			return targetType.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate template target: " + targetType.getName(), e);
		}
	}
}


