package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;
import me.whereareiam.configura.template.TemplateRegistry;

import java.lang.reflect.Field;

public final class ModelTemplateValueResolver implements TemplateValueResolver {
    private final TemplateRegistry templateRegistry;

    public ModelTemplateValueResolver(TemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
    public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
        if (templateRegistry == null) return null;
        TemplateProvider<?> modelProvider = templateRegistry.getTemplateProvider((Class) targetType);
		if (modelProvider == null) return null;

		Object instance = newInstance(targetType);
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


