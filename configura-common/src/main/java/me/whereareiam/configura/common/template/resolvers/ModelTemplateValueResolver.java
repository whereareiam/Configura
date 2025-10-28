package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.template.TemplateRegistry;
import me.whereareiam.configura.common.template.TemplateValueResolver;
import me.whereareiam.configura.TemplateProvider;

import java.lang.reflect.Field;

public final class ModelTemplateValueResolver implements TemplateValueResolver {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		TemplateProvider<?> modelProvider = TemplateRegistry.getModelProvider(targetType);
		if (modelProvider == null) return null;

		return modelProvider.supply((Class) targetType);
	}
}


